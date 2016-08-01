package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.AndroidBoxVolume
import de.qabel.box.storage.BoxVolumeConfig
import de.qabel.box.storage.jdbc.DirectoryMetadataDatabase
import de.qabel.box.storage.jdbc.JdbcDirectoryMetadataFactory
import de.qabel.core.repositories.AndroidVersionAdapter
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.toDocumentId
import de.qabel.qabelbox.storage.server.BlockServer
import java.io.File
import java.sql.Connection

fun makeFileBrowserFactory(identityRepository: IdentityRepository,
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
        val volume = AndroidBoxVolume(BoxVolumeConfig(
                identity.prefixes.first(),
                deviceId,
                backend,
                backend,
                "Blake2b",
                tempDir,
                directoryMetadataFactoryFactory = { tempDir, deviceId ->
                    JdbcDirectoryMetadataFactory(tempDir, deviceId, dataBaseFactory)
                }),
                identity.primaryKeyPair)
        val keyAndPrefix = BoxFileBrowser.KeyAndPrefix(key, identity.prefixes.first())
        return BoxFileBrowser(keyAndPrefix,  BoxVolumeInteractor(volume, keyAndPrefix))
    }
}

