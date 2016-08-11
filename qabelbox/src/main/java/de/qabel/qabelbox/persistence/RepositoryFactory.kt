package de.qabel.qabelbox.persistence

import android.content.Context
import android.util.Log

import java.io.File
import java.io.FileOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

import de.qabel.core.config.Contact
import de.qabel.core.config.factory.DefaultIdentityFactory
import de.qabel.core.repository.DropUrlRepository
import de.qabel.core.repository.EntityManager
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.sqlite.MigrationException
import de.qabel.core.repository.sqlite.SqliteContactRepository
import de.qabel.core.repository.sqlite.SqliteDropUrlRepository
import de.qabel.core.repository.sqlite.SqliteIdentityRepository
import de.qabel.core.repository.sqlite.SqlitePrefixRepository
import de.qabel.core.repository.sqlite.hydrator.DropURLHydrator
import de.qabel.core.repository.sqlite.hydrator.IdentityHydrator
import de.qabel.core.repositories.AndroidClientDatabase
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.sqlite.schemas.ContactDB
import de.qabel.qabelbox.exceptions.QblPersistenceException

class RepositoryFactory(private val context: Context) {
    private var connection: Connection? = null
    private var androidClientDatabase: AndroidClientDatabase? = null
    private var entityManager: EntityManager? = null

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

    val databasePath: File
        get() = context.getFileStreamPath(DB_REPOSITORIES)

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
        entityManager.let {
            if (it == null) {
                val manager = EntityManager()
                entityManager = manager
                return manager
            } else {
                return it
            }
        }
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

    fun getContactRepository(clientDatabase: AndroidClientDatabase): ContactRepository {
        return SqliteContactRepository(
                clientDatabase,
                getEntityManager(),
                getSqliteDropUrlRepository(clientDatabase),
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
        val conn = connection ?:
            try {
                val c = DriverManager.getConnection("jdbc:sqlite:" + databasePath)
                connection = c
                c
            } catch (e: SQLException) {
                throw QblPersistenceException(e)
            }
        val database = androidClientDatabase ?: AndroidClientDatabase(conn).apply {
            migrate()
            androidClientDatabase = this
        }
        return database
    }

    fun close() {
        connection?.let {
            try {
                it.close()
                connection = null
            } catch (e: SQLException) {
                Log.e(TAG, "Could not close connection for AndroidClientDatabase")
            }

            androidClientDatabase = null
        }
    }

    companion object {

        protected val DB_REPOSITORIES = "client-database"
        private val TAG = "RepositorySQLite"
    }
}
