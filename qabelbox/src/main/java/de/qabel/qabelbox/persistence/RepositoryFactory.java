package de.qabel.qabelbox.persistence;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import de.qabel.desktop.config.factory.DefaultIdentityFactory;
import de.qabel.desktop.repository.EntityManager;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;
import de.qabel.desktop.repository.sqlite.SqliteContactRepository;
import de.qabel.desktop.repository.sqlite.SqliteDropUrlRepository;
import de.qabel.desktop.repository.sqlite.SqliteIdentityRepository;
import de.qabel.desktop.repository.sqlite.SqlitePrefixRepository;
import de.qabel.desktop.repository.sqlite.hydrator.DropURLHydrator;
import de.qabel.desktop.repository.sqlite.hydrator.IdentityHydrator;

public class RepositoryFactory {

    protected static final String DB_REPOSITORIES = "client-database";
    private static final String TAG = "RepositorySQLite";
    private Context context;
    private Connection connection;
    private AndroidClientDatabase androidClientDatabase;
    private EntityManager entityManager;

    public RepositoryFactory(Context context) {
        this.context = context;
        loadDriver();
    }

    private void loadDriver() {
        try {
            Class.forName("org.sqldroid.SQLDroidDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    File getDatabasePath() {
        return context.getFileStreamPath(DB_REPOSITORIES);
    }

    public void deleteDatabase() {
        try {
            FileOutputStream outputStream = context.openFileOutput(DB_REPOSITORIES, Context.MODE_PRIVATE);
            outputStream.write(1);
            outputStream.close();
        } catch (java.io.IOException ignored) {
        }
        context.deleteFile(DB_REPOSITORIES);
    }


    public IdentityRepository getIdentityRepository(AndroidClientDatabase clientDatabase) {
		SqliteDropUrlRepository dropUrlRepository = getSqliteDropUrlRepository(clientDatabase);
		SqlitePrefixRepository prefixRepository = getSqlitePrefixRepository(clientDatabase);
		IdentityHydrator hydrator = getIdentityHydrator(getEntityManager(), dropUrlRepository, prefixRepository);
		return new SqliteIdentityRepository(
				clientDatabase, hydrator, dropUrlRepository, prefixRepository);
    }

    @NonNull
    public EntityManager getEntityManager() {
        if (entityManager == null) {
            entityManager = new EntityManager();
        }
        return entityManager;
    }

    @NonNull
    private IdentityHydrator getIdentityHydrator(EntityManager em,
                                                 SqliteDropUrlRepository dropUrlRepository,
                                                 SqlitePrefixRepository prefixRepository) {
        return new IdentityHydrator(
                new DefaultIdentityFactory(),
                em,
                dropUrlRepository,
                prefixRepository
        );
    }

    @NonNull
    public SqliteContactRepository getContactRepository(AndroidClientDatabase clientDatabase) {
        return new SqliteContactRepository(clientDatabase, getEntityManager());
    }

    @NonNull
    public SqlitePrefixRepository getSqlitePrefixRepository(AndroidClientDatabase clientDatabase) {
        return new SqlitePrefixRepository(clientDatabase);
    }

    @NonNull
    public SqliteDropUrlRepository getSqliteDropUrlRepository(AndroidClientDatabase clientDatabase) {
        return new SqliteDropUrlRepository(clientDatabase, new DropURLHydrator());
    }

    @NonNull
    public AndroidClientDatabase getAndroidClientDatabase() throws SQLException {
        if (connection == null) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + getDatabasePath());
            androidClientDatabase = new AndroidClientDatabase(connection);
        }
        return androidClientDatabase;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                Log.e(TAG, "Could not close connection for AndroidClientDatabase");
            }
            androidClientDatabase = null;
        }
    }
}
