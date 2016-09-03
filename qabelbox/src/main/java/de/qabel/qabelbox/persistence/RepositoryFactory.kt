package de.qabel.qabelbox.persistence

import android.content.Context
import android.util.Log
import de.qabel.chat.repository.ChatDropMessageRepository
import de.qabel.chat.repository.ChatShareRepository
import de.qabel.chat.repository.sqlite.SqliteChatDropMessageRepository
import de.qabel.chat.repository.sqlite.SqliteChatShareRepository
import de.qabel.core.config.factory.DefaultIdentityFactory
import de.qabel.core.repositories.AndroidClientDatabase
import de.qabel.core.repository.*
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
    val entityManager: EntityManager by lazy { EntityManager() }

    val databasePath: File get() = context.getFileStreamPath(DB_REPOSITORIES)

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

    private fun close() {
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

    fun getIdentityRepository(): IdentityRepository {
        val dropUrlRepository = getDropUrlRepository()
        val prefixRepository = getSqlitePrefixRepository()
        val hydrator = IdentityHydrator(DefaultIdentityFactory(), entityManager,
                dropUrlRepository as SqliteDropUrlRepository, prefixRepository)
        return SqliteIdentityRepository(getAndroidClientDatabase(), hydrator,
                dropUrlRepository, prefixRepository)
    }

    fun getContactRepository(): ContactRepository =
            SqliteContactRepository(getAndroidClientDatabase(),
                    entityManager,
                    getDropUrlRepository(),
                    getIdentityRepository())

    fun getSqlitePrefixRepository(): SqlitePrefixRepository =
            SqlitePrefixRepository(getAndroidClientDatabase())


    fun getDropUrlRepository(): DropUrlRepository =
            SqliteDropUrlRepository(getAndroidClientDatabase(), DropURLHydrator())

    fun getChatDropMessageRepository(): ChatDropMessageRepository =
            SqliteChatDropMessageRepository(getAndroidClientDatabase(), entityManager)

    fun getDropStateRepository(): DropStateRepository =
            SqliteDropStateRepository(getAndroidClientDatabase(), entityManager)

    fun getChatShareRepository() : ChatShareRepository =
            SqliteChatShareRepository(getAndroidClientDatabase(), entityManager)

}
