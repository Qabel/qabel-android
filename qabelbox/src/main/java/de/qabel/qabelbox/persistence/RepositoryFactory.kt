package de.qabel.qabelbox.persistence

import android.content.Context
import android.util.Log
import de.qabel.core.config.factory.DefaultIdentityFactory
import de.qabel.core.repositories.AndroidClientDatabase
import de.qabel.core.repository.EntityManager
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.sqlite.*
import de.qabel.core.repository.sqlite.hydrator.DropURLHydrator
import de.qabel.core.repository.sqlite.hydrator.IdentityHydrator
import de.qabel.qabelbox.exceptions.QblPersistenceException
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class RepositoryFactory(private val context: Context) {

    private var connection: Connection? = null
    private var androidClientDatabase: AndroidClientDatabase? = null
    private var entityManager: EntityManager? = null

    companion object {
        private const val DB_REPOSITORIES = "client-database"
        private val TAG = "RepositorySQLite"
    }

    init {
        loadDriver()
    }

    private fun loadDriver() {
        try {
            Class.forName("org.sqldroid.SQLDroidDriver")
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(e)
        }

    }

    val databasePath: File get() = context.getFileStreamPath(DB_REPOSITORIES)

    fun deleteDatabase() {
        close()
        try {
            val outputStream = context.openFileOutput(DB_REPOSITORIES, Context.MODE_PRIVATE)
            outputStream.write(1)
            outputStream.close()
        } catch (ignored: java.io.IOException) {
        }

        if (!context.deleteFile(DB_REPOSITORIES)) {
            throw IllegalStateException("Could not delete client database")
        }
    }


    fun getIdentityRepository(clientDatabase: AndroidClientDatabase): IdentityRepository {
        val dropUrlRepository = getSqliteDropUrlRepository(clientDatabase)
        val prefixRepository = getSqlitePrefixRepository(clientDatabase)
        val hydrator = getIdentityHydrator(getEntityManager(), dropUrlRepository, prefixRepository)
        return SqliteIdentityRepository(
                clientDatabase, hydrator, dropUrlRepository, prefixRepository)
    }

    fun getEntityManager(): EntityManager {
        if (entityManager == null) {
            entityManager = EntityManager()
        }
        return entityManager!!
    }

    private fun getIdentityHydrator(em: EntityManager,
                                    dropUrlRepository: SqliteDropUrlRepository,
                                    prefixRepository: SqlitePrefixRepository): IdentityHydrator {
        return IdentityHydrator(
                DefaultIdentityFactory(),
                em,
                dropUrlRepository,
                prefixRepository)
    }

    fun getContactRepository(clientDatabase: AndroidClientDatabase): SqliteContactRepository {
        return SqliteContactRepository(clientDatabase,
                getEntityManager(),
                SqliteDropUrlRepository(clientDatabase),
                getIdentityRepository(clientDatabase))
    }

    fun getSqlitePrefixRepository(clientDatabase: AndroidClientDatabase): SqlitePrefixRepository {
        return SqlitePrefixRepository(clientDatabase)
    }

    fun getSqliteDropUrlRepository(clientDatabase: AndroidClientDatabase): SqliteDropUrlRepository {
        return SqliteDropUrlRepository(clientDatabase, DropURLHydrator())
    }

    @Throws(QblPersistenceException::class)
    fun getAndroidClientDatabase(): AndroidClientDatabase {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)
            } catch (e: SQLException) {
                throw QblPersistenceException(e)
            }

            androidClientDatabase = AndroidClientDatabase(connection!!)
            try {
                androidClientDatabase!!.migrate()
            } catch (e: MigrationException) {
                throw RuntimeException(e)
            }

        }
        return androidClientDatabase!!
    }

    fun close() {
        if (connection != null) {
            try {
                connection!!.close()
                connection = null
            } catch (e: SQLException) {
                Log.e(TAG, "Could not close connection for AndroidClientDatabase")
            }

            androidClientDatabase = null
        }
    }

}
