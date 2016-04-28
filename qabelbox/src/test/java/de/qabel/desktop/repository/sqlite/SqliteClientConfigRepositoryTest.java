package de.qabel.desktop.repository.sqlite;

import de.qabel.desktop.repository.ClientConfigRepository;
import de.qabel.desktop.repository.EntityManager;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SqliteClientConfigRepositoryTest extends AbstractSqliteRepositoryTest<ClientConfigRepository> {
    @Override
    protected ClientConfigRepository createRepo(ClientDatabase clientDatabase, EntityManager em) throws Exception {
        return new SqliteClientConfigRepository(clientDatabase);
    }

    @Test(expected = EntityNotFoundExcepion.class)
    public void throwsExceptionWhenKeyIsNotFound() throws Exception {
        repo.find("does not exist");
    }

    @Test
    public void findsSavedValue() throws Exception {
        repo.save("key", "value");
        assertEquals("value", repo.find("key"));
        assertTrue(repo.contains("key"));
    }

    @Test
    public void knowsMissingValues() throws Exception {
        assertFalse(repo.contains("key"));
    }

    @Test
    public void knowsNullValuesAsMissingValues() throws Exception {
        repo.save("key", "value");
        repo.save("key", null);
        assertFalse(repo.contains("key"));
    }
}
