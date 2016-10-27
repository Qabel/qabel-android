package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.AndroidBoxVolume
import de.qabel.box.storage.BoxVolumeConfig
import de.qabel.box.storage.RootRefCalculator
import de.qabel.box.storage.jdbc.DirectoryMetadataDatabase
import de.qabel.box.storage.jdbc.JdbcDirectoryMetadataFactory
import de.qabel.box.storage.jdbc.JdbcFileMetadataFactory
import de.qabel.core.repositories.AndroidVersionAdapter
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.toDocumentId
import de.qabel.qabelbox.storage.server.BlockServer
import java.io.File
import java.sql.Connection

object JdbcPrefix {
    @JvmField
    var jdbcPrefix = "jdbc:sqldroid:"
}

fun makeFileBrowserFactory(identityRepository: IdentityRepository,
                           contactRepository: ContactRepository,
                           deviceId: ByteArray,
                           tempDir: File,
                           androidBlockServer: BlockServer):
        (VolumeRoot) -> FileBrowser {
    return fun(volumeRoot: VolumeRoot): FileBrowser {
        val docId = volumeRoot.documentID.toDocumentId()
        val key = docId.identityKey
        val identity = identityRepository.find(key)
        val backend = BoxHttpStorageBackend(androidBlockServer, docId.prefix)
        val dataBaseFactory: (Connection) -> DirectoryMetadataDatabase = { connection ->
            DirectoryMetadataDatabase(connection, AndroidVersionAdapter(connection))
        }
        val prefix = identity.prefixes.first()
        val prefixKey = prefix.prefix
        val rootRef = RootRefCalculator().rootFor(
                identity.primaryKeyPair.privateKey,
                prefix.type,
                prefixKey
        )
        val volume = AndroidBoxVolume(BoxVolumeConfig(
                prefixKey,
                rootRef,
                deviceId,
                backend,
                backend,
                "Blake2b",
                tempDir,
                directoryMetadataFactoryFactory = { tempDir, deviceId ->
                    JdbcDirectoryMetadataFactory(tempDir, deviceId, dataBaseFactory, JdbcPrefix.jdbcPrefix)
                },
                fileMetadataFactoryFactory = { tempDir ->
                    JdbcFileMetadataFactory(tempDir, ::AndroidVersionAdapter, JdbcPrefix.jdbcPrefix)
                }),
                identity.primaryKeyPair)
        return BoxFileBrowser(BoxFileBrowser.KeyAndPrefix(identity), volume, contactRepository)
    }
}

