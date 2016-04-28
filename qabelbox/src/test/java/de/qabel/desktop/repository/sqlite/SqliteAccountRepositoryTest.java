package de.qabel.desktop.repository.sqlite;

import de.qabel.core.config.Account;
import de.qabel.desktop.repository.EntityManager;
import de.qabel.qabelbox.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class SqliteAccountRepositoryTest extends AbstractSqliteRepositoryTest<SqliteAccountRepository> {

    @Override
    protected SqliteAccountRepository createRepo(ClientDatabase clientDatabase, EntityManager em) throws Exception {
        return new SqliteAccountRepository(clientDatabase, em);
    }

    @Test
    public void findsSavedAccount() throws Exception {
        Account account = new Account("p", "u", "a");
        repo.save(account);

        List<Account> accounts = repo.findAll();

        assertEquals(1, accounts.size());
        assertSame(account, accounts.get(0));
    }

    @Test
    public void findsUncachedAccounts() throws Exception {
        Account account = new Account("p", "u", "a");
        repo.save(account);
        em.clear();

        List<Account> accounts = repo.findAll();
        assertEquals(1, accounts.size());
        Account loaded = accounts.get(0);
        assertEquals("p", loaded.getProvider());
        assertEquals("u", loaded.getUser());
        assertEquals("a", loaded.getAuth());
        assertEquals(account.getId(), loaded.getId());
    }

    @Test
    public void providesInternalReferenceApi() throws Exception {
        Account account = new Account("p", "u", "a");
        repo.save(account);

        assertSame(account, repo.find(String.valueOf(account.getId())));
    }

    @Test
    public void updatesExistingAccounts() throws Exception {
        Account account = new Account("p", "u", "a");
        repo.save(account);
        account.setAuth("777");
        repo.save(account);

        assertEquals(1, repo.findAll().size());
        assertEquals("777", repo.findAll().get(0).getAuth());
    }
}
