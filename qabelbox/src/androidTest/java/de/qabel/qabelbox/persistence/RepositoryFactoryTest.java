package de.qabel.qabelbox.persistence;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.qabel.core.repositories.AndroidClientDatabase;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RepositoryFactoryTest {

    Context mMockContext;
    private RepositoryFactory repositoryFactory;

    @Before
    public void setUp() {
        mMockContext = new RenamingDelegatingContext(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), "factorytest_");
        repositoryFactory = new RepositoryFactory(mMockContext);
        repositoryFactory.deleteDatabase();
    }

    @After
    public void tearDown() {
        repositoryFactory.deleteDatabase();
    }

    @Test
    public void testCanDeleteDatabase() throws Exception {
        // raises an exception on error.
        repositoryFactory.deleteDatabase();
    }

    @Test
    public void testGetDatabasePath() throws Exception {
        assertThat(repositoryFactory.getDatabasePath().getAbsolutePath(),
                allOf(endsWith("files/factorytest_client-database"), startsWith("/data/")));
    }

    @Test
    public void testVersionHandling() throws Exception {
        AndroidClientDatabase database = repositoryFactory.getAndroidClientDatabase();
        long version = 1;
        database.setVersion(version);
        assertThat("Could not set database version",
                database.getVersion(), is(version));
    }

    @Test
    public void testGetRepositories() {
        assertThat(repositoryFactory.getChatDropMessageRepository(), notNullValue());
        assertThat(repositoryFactory.getContactRepository(), notNullValue());
        assertThat(repositoryFactory.getDropStateRepository(), notNullValue());
        assertThat(repositoryFactory.getDropUrlRepository(), notNullValue());
        assertThat(repositoryFactory.getIdentityRepository(), notNullValue());
        assertThat(repositoryFactory.getSqlitePrefixRepository(), notNullValue());
    }

}
