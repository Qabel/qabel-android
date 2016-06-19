package de.qabel.desktop.repository.sqlite.hydrator;

import org.spongycastle.util.encoders.Hex;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropURL;
import de.qabel.desktop.config.factory.ContactFactory;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.repository.sqlite.SqliteDropUrlRepository;
import kotlin.Pair;

public class ContactIdentitiesHydrator extends AbstractHydrator<Pair<Contact, List<Identity>>> {

    private ContactFactory contactFactory;
    private SqliteDropUrlRepository dropUrlRepository;

    private Identities identities;
    private Map<String, String> identityKeyMap;

    public ContactIdentitiesHydrator(Identities identities, Map<String, String> identityKeyMap, ContactFactory contactFactory, SqliteDropUrlRepository dropUrlRepository) {
        this.contactFactory = contactFactory;
        this.dropUrlRepository = dropUrlRepository;

        this.identities = identities;
        this.identityKeyMap = identityKeyMap;
    }

    public String[] getGroupByFields(String tableAlias) {
        return super.getFields(tableAlias);
    }

    @Override
    public String[] getFields(String... tableAlias) {
        String[] fields = super.getFields(tableAlias);
        String[] allFields = new String[fields.length + identityKeyMap.size()];
        int i = 0;
        for (String field : fields) {
            allFields[i] = field;
            i++;
        }
        for (String queryAlias : identityKeyMap.keySet()) {
            allFields[i] = "count(" + queryAlias + ".id)";
            i++;
        }
        return allFields;
    }

    @Override
    protected String[] getFields() {
        return new String[]{"id", "publicKey", "alias", "phone", "email"};
    }

    public Pair<Contact, List<Identity>> hydrateOne(ResultSet resultSet) throws SQLException {
        int column = 1;
        int id = resultSet.getInt(column++);
        String publicKeyAsHex = resultSet.getString(column++);
        String alias = resultSet.getString(column++);
        String phone = resultSet.getString(column++);
        String email = resultSet.getString(column++);
        List<Identity> contactIdentities = new LinkedList<>();
        for (Map.Entry<String, String> identityEntry : identityKeyMap.entrySet()) {
            if (resultSet.getInt(column++) > 0) {
                contactIdentities.add(identities.getByKeyIdentifier(identityEntry.getValue()));
            }
        }
        QblECPublicKey publicKey = new QblECPublicKey(Hex.decode(publicKeyAsHex));
        Contact contact = contactFactory.createContact(publicKey, new LinkedList<DropURL>(), alias);
        contact.setId(id);
        contact.setPhone(phone);
        contact.setEmail(email);
        try {
            for (DropURL url : dropUrlRepository.findAll(contact)) {
                contact.addDrop(url);
            }
        } catch (PersistenceException e) {
            throw new SQLException("Failed to load DropUrls for contact: " + e.getMessage(), e);
        }

        return new Pair<>(contact, contactIdentities);
    }

    @Override
    public void recognize(Pair<Contact, List<Identity>> instance) {

    }
}
