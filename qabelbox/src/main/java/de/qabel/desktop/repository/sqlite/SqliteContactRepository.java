package de.qabel.desktop.repository.sqlite;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropURL;
import de.qabel.desktop.StringUtils;
import de.qabel.desktop.config.factory.DefaultContactFactory;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.EntityManager;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.repository.sqlite.hydrator.ContactHydrator;
import de.qabel.desktop.repository.sqlite.hydrator.ContactIdentitiesHydrator;
import de.qabel.desktop.repository.sqlite.hydrator.DropURLHydrator;
import de.qabel.desktop.repository.sqlite.hydrator.SimpleContactHydrator;
import kotlin.Pair;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SqliteContactRepository extends AbstractSqliteRepository<Contact> implements ContactRepository {

    public static final String TABLE_NAME = "contact";

    private final SqliteDropUrlRepository dropUrlRepository;
    private final IdentityRepository identityRepository;
    private final EntityManager entityManager;

    public SqliteContactRepository(ClientDatabase database, EntityManager em, IdentityRepository identityRepository) {
        super(database, new ContactHydrator(
                em,
                new DefaultContactFactory(),
                new SqliteDropUrlRepository(database, new DropURLHydrator())
        ), TABLE_NAME);

        this.dropUrlRepository = new SqliteDropUrlRepository(database, new DropURLHydrator());
        this.identityRepository = identityRepository;
        this.entityManager = em;
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
    public Contact findByKeyId(String keyId) throws EntityNotFoundExcepion {
        try {
            try (PreparedStatement statement = database.prepare(
                    "SELECT " + StringUtils.join(",", hydrator.getFields("c")) + " FROM " + TABLE_NAME + " c " +
                            "WHERE c.publicKey = ? " +
                            "LIMIT 1"
            )) {
                statement.setString(1, keyId);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        throw new EntityNotFoundExcepion(
                                "no contact found for key '" + keyId + "'");
                    }
                    return hydrator.hydrateOne(results);
                }
            }
        } catch (SQLException e) {
            throw new EntityNotFoundExcepion("exception while searching contact: " + e.getMessage(), e);
        }
    }

    @Override
    public Pair<Contact, List<Identity>> findContactWithIdentities(String key) throws PersistenceException, EntityNotFoundExcepion {
        Contact contact = findByKeyId(key);

        Identities identities = identityRepository.findAll();
        Map<Integer, List<String>> associatedIdentities = findContactIdentityKeys(
                Collections.singletonList(contact.getId()));
        List<Identity> contactIdentities;
        if (associatedIdentities.containsKey(contact.getId())) {
            contactIdentities = new ArrayList<>(associatedIdentities.get(contact.getId()).size());
            for (String identityKey : associatedIdentities.get(contact.getId())) {
                contactIdentities.add(identities.getByKeyIdentifier(identityKey));
            }
        } else {
            contactIdentities = Collections.emptyList();
        }
        return new Pair<>(contact, contactIdentities);
    }


    private Collection<Contact> find(String searchString) throws PersistenceException {
        SimpleContactHydrator hydrator = new SimpleContactHydrator(entityManager);
        StringBuilder queryBuilder = new StringBuilder(
                "SELECT " + StringUtils.join(",", hydrator.getFields("c")) + " " +
                        "FROM contact c ");

        boolean filterResults = (searchString != null && !searchString.trim().isEmpty());
        if (filterResults) {
            queryBuilder.append("WHERE (lower(c.alias) LIKE ? OR c.phone LIKE ? OR c.email LIKE ?)");
        }

        try (PreparedStatement statement = database.prepare(queryBuilder.toString())) {
            int paramIndex = 1;
            if (filterResults) {
                String lowerWildSearchString = searchString.toLowerCase() + "%";
                statement.setString(paramIndex++, lowerWildSearchString);
                statement.setString(paramIndex++, lowerWildSearchString);
                statement.setString(paramIndex++, lowerWildSearchString);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return hydrator.hydrateAll(resultSet);
            }
        } catch (SQLException e) {
            throw new PersistenceException("failed to load all contacts", e);
        }
    }

    private Map<Integer, List<String>> findContactIdentityKeys(List<Integer> contactIds) throws PersistenceException {
        try {
            Map<Integer, List<String>> contactIdentityMap = new HashMap<>();
            try (PreparedStatement statement = database.prepare(
                    "SELECT c.id, c2.publicKey " +
                            "FROM " + TABLE_NAME + " c " +
                            "JOIN identity_contacts ic ON (c.id = ic.contact_id) " +
                            "JOIN identity i ON (i.id = ic.identity_id) " +
                            "JOIN contact c2 ON (c2.id = i.contact_id) " +
                            "WHERE c.id IN (" + StringUtils.join(",", contactIds) + ")"
            )) {
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        int id = results.getInt(1);
                        List<String> identityKeys = contactIdentityMap.get(id);
                        if (identityKeys == null) {
                            identityKeys = new LinkedList<>();
                            contactIdentityMap.put(id, identityKeys);
                        }
                        identityKeys.add(results.getString(2));
                    }
                }
                return contactIdentityMap;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error loading identities for contacts");
        }
    }

    @Override
    public Collection<Pair<Contact, List<Identity>>> findWithIdentities(String searchString) throws PersistenceException {

        Identities identities = identityRepository.findAll();
        Collection<Contact> contacts = find(searchString);
        List<Integer> contactsIDs = new ArrayList<>(contacts.size());
        for (Contact c : contacts) {
            contactsIDs.add(c.getId());
        }

        Map<Integer, List<DropURL>> contactsDropUrls = dropUrlRepository.findDropUrls(contactsIDs);
        Map<Integer, List<String>> contactIdentityKeys = findContactIdentityKeys(contactsIDs);

        List<Pair<Contact, List<Identity>>> resultList = new ArrayList<>(contacts.size());
        for (Contact c : contacts) {
            List<DropURL> dropURLs = contactsDropUrls.get(c.getId());
            if (dropURLs != null) {
                for (DropURL dropURL : dropURLs) {
                    c.addDrop(dropURL);
                }
            }

            List<String> contactIdentKeys = contactIdentityKeys.get(c.getId());
            List<Identity> contactIdentities;
            if (contactIdentKeys != null) {
                contactIdentities = new ArrayList<>(contactIdentKeys.size());
                for (String identityKey : contactIdentKeys) {
                    contactIdentities.add(identities.getByKeyIdentifier(identityKey));
                }
            } else {
                contactIdentities = Collections.emptyList();
            }
            resultList.add(new Pair(c, contactIdentities));
        }
        return resultList;
    }
}
