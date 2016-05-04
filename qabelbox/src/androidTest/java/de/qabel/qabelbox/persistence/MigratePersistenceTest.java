package de.qabel.qabelbox.persistence;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.desktop.config.factory.DropUrlGenerator;
import de.qabel.desktop.config.factory.IdentityBuilder;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;
import de.qabel.desktop.repository.sqlite.SqliteContactRepository;
import de.qabel.qabelbox.QabelBoxApplication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
        factory.deleteDatabase();
        AndroidClientDatabase androidClientDatabase = factory.getAndroidClientDatabase();
        identityRepository = factory.getIdentityRepository(androidClientDatabase);
        contactRepository = factory.getContactRepository(androidClientDatabase);

        QblSQLiteParams params = new QblSQLiteParams(context, "migrate-test", null, 1);
		androidPersistence = new AndroidPersistence(params);
        androidPersistence.dropTable(Identity.class);
        androidPersistence.dropTable(Contacts.class);

        IdentityBuilder identityBuilder = new IdentityBuilder(
                new DropUrlGenerator(QabelBoxApplication.DEFAULT_DROP_SERVER));
        first = identityBuilder.withAlias("first").build();
        second = identityBuilder.withAlias("second").build();
        androidPersistence.persistEntity(first);
        androidPersistence.persistEntity(second);
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
}
