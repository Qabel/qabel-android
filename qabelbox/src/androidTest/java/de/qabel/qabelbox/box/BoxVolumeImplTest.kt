package de.qabel.qabelbox.box

import de.qabel.box.storage.AbstractMetadata
import de.qabel.box.storage.AndroidBoxVolume
import de.qabel.box.storage.BoxVolumeConfig
import de.qabel.box.storage.BoxVolumeImpl
import de.qabel.box.storage.jdbc.DirectoryMetadataDatabase
import de.qabel.box.storage.jdbc.JdbcDirectoryMetadataFactory
import de.qabel.core.repositories.AndroidVersionAdapter
import de.qabel.qabelbox.box.backends.MockStorageBackend
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import java.sql.DriverManager

class BoxVolumeImplTest {

    val identity = IdentityHelper.createIdentity("identity", "prefix")

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
                    JdbcDirectoryMetadataFactory(tempDir, deviceId) { connection ->
                        DirectoryMetadataDatabase(connection, AndroidVersionAdapter(connection))
                    }
                }),
                identity.primaryKeyPair )
        volume.createIndex("qabel", "prefix")
        volume.navigate()
    }

    @Test
    fun testDM() {
        val connection = DriverManager.getConnection(
                AbstractMetadata.JDBC_PREFIX + createTempFile().absolutePath)
        connection.autoCommit = true
        val db = DirectoryMetadataDatabase(connection,
                versionAdapter = AndroidVersionAdapter(connection))
        db.migrate()
        db.migrate()
    }
}

