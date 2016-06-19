package de.qabel.desktop.repository.sqlite;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.desktop.StringUtils;
import de.qabel.desktop.config.factory.DefaultContactFactory;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.EntityManager;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.repository.sqlite.hydrator.ContactHydrator;
import de.qabel.desktop.repository.sqlite.hydrator.ContactIdentitiesHydrator;
import de.qabel.desktop.repository.sqlite.hydrator.DropURLHydrator;
import kotlin.Pair;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SqliteContactRepository extends AbstractSqliteRepository<Contact> implements ContactRepository {
    public static final String TABLE_NAME = "contact";
    private final SqliteDropUrlRepository dropUrlRepository;

    public SqliteContactRepository(ClientDatabase database, EntityManager em) {
        this(
                database,
                new ContactHydrator(
                        em,
                        new DefaultContactFactory(),
                        new SqliteDropUrlRepository(database, new DropURLHydrator())
                ),
                new SqliteDropUrlRepository(database, new DropURLHydrator())
        );
    }

    public SqliteContactRepository(ClientDatabase database, Hydrator<Contact> hydrator, SqliteDropUrlRepository dropUrlRepository) {
        super(database, hydrator, TABLE_NAME);
        this.dropUrlRepository = dropUrlRepository;
    }

    Contact find(Integer id) throws PersistenceException, EntityNotFoundExcepion {
        return findBy("id=?", id);
    }

    @Override
    public synchronized Contacts find(Identity identity) throws PersistenceException {
        Contacts contacts = new Contacts(identity);

        try (PreparedStatement statement = database.prepare(
                "SELECT " + StringUtils.join(",", hydrator.getFields("c")) + " " +
                        "FROM contact c " +
                        "JOIN identity_contacts ic ON (c.id = ic.contact_id) " +
                        "JOIN identity i ON (ic.identity_id = i.id) " +
                        "JOIN contact c2 ON (i.contact_id = c2.id) " +
                        "WHERE c2.publicKey = ?"
        )) {
            statement.setString(1, identity.getKeyIdentifier());
            try (ResultSet resultSet = statement.executeQuery()) {
                for (Contact c : hydrator.hydrateAll(resultSet)) {
                    contacts.put(c);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("failed to load contacts for " + identity, e);
        }

        return contacts;
    }

    @Override
    public synchronized void save(Contact contact, Identity identity) throws PersistenceException {
        try {
            if (contact.getId() == 0 || !exists(contact)) {
                insert(contact, identity);
            } else {
                update(contact, identity);
            }
            dropUrlRepository.delete(contact);
            dropUrlRepository.store(contact);
            hydrator.recognize(contact);
        } catch (SQLException e) {
            throw new PersistenceException("failed to save contact: " + e.getMessage(), e);
        }
    }

    private boolean exists(Contact contact) throws SQLException {
        try (PreparedStatement statement = database.prepare("SELECT id FROM contact WHERE id = ?")) {
            statement.setInt(1, contact.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insert(Contact contact, Identity identity) throws SQLException {
        try {
            Contact existing = findBy("publicKey=?", contact.getKeyIdentifier());
            contact.setId(existing.getId());
        } catch (PersistenceException | EntityNotFoundExcepion e) {
            try (PreparedStatement statement = database.prepare(
                    "INSERT INTO contact (publicKey, alias, phone, email) VALUES (?, ?, ?, ?)"
            )) {
                int i = 1;
                statement.setString(i++, contact.getKeyIdentifier());
                statement.setString(i++, contact.getAlias());
                statement.setString(i++, contact.getPhone());
                statement.setString(i++, contact.getEmail());
                statement.execute();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    contact.setId(keys.getInt(1));
                }
            }
        }
        insertConnection(contact, identity);
    }

    private void insertConnection(Contact contact, Identity identity) throws SQLException {
        try (PreparedStatement statement = database.prepare(
                "INSERT OR IGNORE INTO identity_contacts (identity_id, contact_id) VALUES (?, ?)"
        )) {
            int i = 1;
            statement.setInt(i++, identity.getId());
            statement.setInt(i++, contact.getId());
            statement.execute();
        }
    }

    private void update(Contact contact, Identity identity) throws SQLException {
        try (PreparedStatement statement = database.prepare(
                "UPDATE contact SET publicKey=?, alias=?, phone=?, email=? WHERE id=?"
        )) {
            int i = 1;
            statement.setString(i++, contact.getKeyIdentifier());
            statement.setString(i++, contact.getAlias());
            statement.setString(i++, contact.getPhone());
            statement.setString(i++, contact.getEmail());
            statement.setInt(i++, contact.getId());
            statement.execute();
        }
        insertConnection(contact, identity);
    }

    @Override
    public synchronized void delete(Contact contact, Identity identity) throws PersistenceException, EntityNotFoundExcepion {
        try {
            try (PreparedStatement statement = database.prepare(
                    "DELETE FROM identity_contacts WHERE contact_id = ? AND identity_id = ?"
            )) {
                int i = 1;
                statement.setInt(i++, contact.getId());
                statement.setInt(i++, identity.getId());
                statement.execute();
                if (statement.getUpdateCount() != 1) {
                    throw new EntityNotFoundExcepion(
                            "Contact " + contact.getAlias() + " for identity "
                                    + identity.getAlias() + " not found");
                }
            }
            try (PreparedStatement statement = database.prepare(
                    "DELETE FROM contact WHERE id = ? AND NOT EXISTS (" +
                            "SELECT contact_id FROM identity_contacts WHERE contact_id = ? LIMIT 1" +
                            ")"
            )) {
                statement.setInt(1, contact.getId());
                statement.setInt(2, contact.getId());
                statement.execute();
            }
        } catch (SQLException e) {
            throw new PersistenceException("failed to delete contact", e);
        }
    }

    @Override
    public synchronized Contact findByKeyId(Identity identity, String keyId) throws EntityNotFoundExcepion {
        try {
            try (PreparedStatement statement = database.prepare(
                    "SELECT " + StringUtils.join(",", hydrator.getFields("c")) + " FROM " + TABLE_NAME + " c " +
                            "JOIN identity_contacts ic ON (c.id = ic.contact_id) " +
                            "JOIN identity i ON (i.id = ic.identity_id) " +
                            "JOIN contact c2 ON (c2.id = i.contact_id) " +
                            "WHERE c2.publicKey = ? AND c.publicKey = ? " +
                            "LIMIT 1"
            )) {
                statement.setString(1, identity.getKeyIdentifier());
                statement.setString(2, keyId);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        throw new EntityNotFoundExcepion(
                                "no contact found for identity '" + identity.getAlias() + "' and key '" + keyId + "'");
                    }
                    return hydrator.hydrateOne(results);
                }
            }
        } catch (SQLException e) {
            throw new EntityNotFoundExcepion("exception while searching contact: " + e.getMessage(), e);
        }
    }

    @Override
    public Collection<Pair<Contact, List<Identity>>> find(Identities identities, String searchString) throws PersistenceException {
        try {

            Map<String, String> identityKeyMap = new HashMap<>();
            for (Identity identity : identities.getIdentities()) {
                String key = "id_" + identity.getId();
                identityKeyMap.put(key, identity.getKeyIdentifier());
            }

            ContactIdentitiesHydrator customHydrator = new ContactIdentitiesHydrator(
                    identities, identityKeyMap, new DefaultContactFactory(),
                    new SqliteDropUrlRepository(database, new DropURLHydrator()));

            StringBuilder selectBuilder = new StringBuilder();
            selectBuilder.append("SELECT " + StringUtils.join(",", customHydrator.getFields("c")));

            StringBuilder fromBuilder = new StringBuilder();
            StringBuilder whereBuilder = new StringBuilder();
            //Remove Identity Contacts
            fromBuilder.append(" FROM " + TABLE_NAME + " c LEFT JOIN identity ident on ident.contact_id = c.id ");
            whereBuilder.append(" WHERE ident.id is null ");

            //Check contact is connected to identities
            for (Map.Entry<String, String> identityEntry : identityKeyMap.entrySet()) {
                String key = identityEntry.getKey();
                String identityContactsAlias = "ic_" + key;
                String identityAlias = "i_" + key;
                fromBuilder.append("LEFT JOIN identity_contacts " + identityContactsAlias +
                        " ON (c.id = " + identityContactsAlias + ".contact_id) ");
                fromBuilder.append("LEFT JOIN identity " + identityAlias +
                        " ON (" + identityAlias + ".id = " + identityContactsAlias + ".identity_id) ");
                fromBuilder.append("LEFT JOIN contact " + key +
                        " ON (" + key + ".id = " + identityAlias + ".contact_id AND " + key + ".publicKey = ?)");
            }
            boolean filterResults = searchString != null && !searchString.trim().isEmpty();
            if (filterResults) {
                whereBuilder.append(" AND (lower(c.alias) LIKE ? OR c.phone LIKE ? OR c.email LIKE ?) ");
            }

            try (PreparedStatement statement = database.prepare(
                    selectBuilder.toString() + fromBuilder.toString() + whereBuilder.toString() +
                            " GROUP BY " + StringUtils.join(", ", customHydrator.getGroupByFields("c")) + " ORDER BY c.alias"
            )) {
                int paramIndex = 1;
                for (String identityKey : identityKeyMap.values()) {
                    statement.setString(paramIndex, identityKey);
                    paramIndex++;
                }
                if (filterResults) {
                    int count = paramIndex + 3;
                    String lowerWildSearchString = searchString.toLowerCase() + "%";
                    while (paramIndex < count) {
                        statement.setString(paramIndex, lowerWildSearchString);
                        paramIndex++;
                    }
                }

                try (ResultSet results = statement.executeQuery()) {
                    return customHydrator.hydrateAll(results);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to load contacts with identites " + e.getMessage(), e);
        }
    }
}
