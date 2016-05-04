package de.qabel.qabelbox.persistence;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.desktop.config.factory.DropUrlGenerator;
import de.qabel.desktop.config.factory.IdentityBuilderFactory;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;
import de.qabel.desktop.repository.sqlite.SqliteContactRepository;
import de.qabel.qabelbox.QabelBoxApplication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(AndroidJUnit4.class)
public class MigratePersistenceTest {

    private RepositoryFactory factory;
    private AndroidPersistence androidPersistence;
    private Identity first;
    private Identity second;
    private IdentityRepository identityRepository;
    private SqliteContactRepository contactRepository;

    @Before
    public void setUp() throws Exception {
        Context context = new RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(),
                "test_");
        factory = new RepositoryFactory(context);
        factory.close();
        factory.deleteDatabase();
        AndroidClientDatabase androidClientDatabase = factory.getAndroidClientDatabase();
        identityRepository = factory.getIdentityRepository(androidClientDatabase);
        assertThat(identityRepository.findAll().getIdentities(), empty());
        contactRepository = factory.getContactRepository(androidClientDatabase);

        QblSQLiteParams params = new QblSQLiteParams(context, "migrate-test", null, 1);
		androidPersistence = new AndroidPersistence(params);
        androidPersistence.dropTable(Identity.class);
        androidPersistence.dropTable(Contacts.class);


        IdentityBuilderFactory builderFactory = new IdentityBuilderFactory(
                new DropUrlGenerator(QabelBoxApplication.DEFAULT_DROP_SERVER));
        first = builderFactory.factory().withAlias("first").build();
        second = builderFactory.factory().withAlias("second").build();
        androidPersistence.persistEntity(first);
        androidPersistence.persistEntity(second);
        assertThat(first.getEcPublicKey(), not(equalTo(second.getEcPublicKey())));
    }

    @After
    public void tearDown() {
        factory.close();
    }

    @Test
    public void testIdentitiesMigrated() throws Exception {
        PersistenceMigrator.migrate(androidPersistence, identityRepository, contactRepository);
        assertThat(identityRepository.findAll().getIdentities().size(), is(2));
        assertThat("Identity 'first' not found", identityRepository.find(first.getKeyIdentifier()),
                notNullValue());
        assertThat("Identity 'second' not found", identityRepository.find(second.getKeyIdentifier()),
                notNullValue());
    }

    @Test
    public void testContactsMigrated() throws Exception {
        Contact contact1 = new Contact("contact1", first.getDropUrls(), first.getEcPublicKey());
        Contact contact2 = new Contact("contact2", second.getDropUrls(), second.getEcPublicKey());
        Contact contact3 = new Contact("contact3", first.getDropUrls(), new QblECKeyPair().getPub());
        Contacts firstContacts = new Contacts(first);
        firstContacts.put(contact2);
        firstContacts.put(contact1);
        firstContacts.put(contact3);
        androidPersistence.persistEntity(firstContacts);
        Contacts secondContacts = new Contacts(second);
        secondContacts.put(contact2);
        secondContacts.put(contact1);
        androidPersistence.persistEntity(secondContacts);

        PersistenceMigrator.migrate(androidPersistence, identityRepository, contactRepository);
        Contacts firstMigrated = contactRepository.find(first);
        assertThat("Not all contacts found for 'first' identity",
                firstMigrated.getContacts().size(), equalTo(3));
        assertThat("contact1 not found in 'first'", firstMigrated.contains(contact1));
        assertThat("contact2 not found in 'first'", firstMigrated.contains(contact2));
        assertThat("contact3 not found in 'first'", firstMigrated.contains(contact3));

        Contacts secondMigrated = contactRepository.find(second);
        assertThat("Not all contacts found for 'second' identity",
                secondMigrated.getContacts().size(), equalTo(2));
        assertThat("contact1 not found in 'second'", secondMigrated.contains(contact1));
        assertThat("contact2 not found in 'second'", secondMigrated.contains(contact2));
    }
}
