package de.qabel.qabelbox.box

import de.qabel.box.storage.AndroidBoxVolume
import de.qabel.box.storage.BoxVolumeConfig
import de.qabel.box.storage.jdbc.DirectoryMetadataDatabase
import de.qabel.box.storage.jdbc.JdbcDirectoryMetadataFactory
import de.qabel.box.storage.jdbc.JdbcFileMetadataFactory
import de.qabel.core.repositories.AndroidVersionAdapter
import de.qabel.qabelbox.box.backends.MockStorageBackend
import de.qabel.qabelbox.box.interactor.JdbcPrefix
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Before
import org.junit.Test
import java.sql.Driver
import java.sql.DriverManager

class AndroidBoxVolumeTest {

    val identity = IdentityHelper.createIdentity("identity", "prefix")

    @Before
    fun loadDriver() {
        try {
            DriverManager.registerDriver(
                    Class.forName("org.sqldroid.SQLDroidDriver").newInstance() as Driver)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun testNavigate() {
        val backend = MockStorageBackend()
        val volume = AndroidBoxVolume(BoxVolumeConfig(
                "prefix",
                byteArrayOf(1,2,3,4),
                backend,
                backend,
                "Blacke2b",
                createTempDir(),
                directoryMetadataFactoryFactory = { tempDir, deviceId ->
                    JdbcDirectoryMetadataFactory(tempDir, deviceId, { connection ->
                        DirectoryMetadataDatabase(connection, AndroidVersionAdapter(connection))
                    }, jdbcPrefix = JdbcPrefix.jdbcPrefix)
                },
                fileMetadataFactoryFactory = { tempDir ->
                    JdbcFileMetadataFactory(tempDir, versionAdapterFactory = { connection ->
                        AndroidVersionAdapter(connection)})
                }
                ),
                identity.primaryKeyPair )
        volume.createIndex("qabel", "prefix")
        volume.navigate()
    }

    @Test
    fun testMigrationRespectsDatabaseVersion() {
        val connection = DriverManager.getConnection(
                JdbcPrefix.jdbcPrefix + createTempFile().absolutePath)
        connection.autoCommit = true
        val db = DirectoryMetadataDatabase(connection,
                versionAdapter = AndroidVersionAdapter(connection))
        db.migrate()
        db.migrate()
    }
}

