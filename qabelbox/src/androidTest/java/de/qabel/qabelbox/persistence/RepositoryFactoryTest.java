package de.qabel.qabelbox.persistence;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RepositoryFactoryTest{

    Context mMockContext;
    private RepositoryFactory repositoryFactory;

    @Before
    public void setUp() {
        mMockContext = new RenamingDelegatingContext(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
        repositoryFactory = new RepositoryFactory(mMockContext);
    }

    @After
    public void tearDown() {
        repositoryFactory.close();
        repositoryFactory.deleteDatabase();
    }

    @Test
    public void testCanDeleteDatabase() throws Exception {
        repositoryFactory.getAndroidClientDatabase().setVersion(1);
        repositoryFactory.deleteDatabase();
        assertEquals(repositoryFactory.getAndroidClientDatabase().getVersion(), 0L);
    }

    @Test
    public void testGetDatabasePath() throws Exception {
        assertThat(repositoryFactory.getDatabasePath().getAbsolutePath(),
                allOf(endsWith("files/test_client-database"), startsWith("/data/")));
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
    public void testMigrate() throws Exception {
        AndroidClientDatabase database = repositoryFactory.getAndroidClientDatabase();
        assertThat("Database version doesn't start at 0",
                database.getVersion(), is(0L));
        database.migrate();
        assertThat("Database version not changed after migrate()",
                database.getVersion(), is(1460987825L));
    }

}
