package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.AndroidBoxVolume
import de.qabel.box.storage.BoxVolumeConfig
import de.qabel.box.storage.RootRefCalculator
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.jdbc.DirectoryMetadataDatabase
import de.qabel.box.storage.jdbc.JdbcDirectoryMetadataFactory
import de.qabel.box.storage.jdbc.JdbcFileMetadataFactory
import de.qabel.client.box.BoxSchedulers
import de.qabel.client.box.documentId.toDocumentId
import de.qabel.client.box.interactor.*
import de.qabel.client.box.storage.LocalStorage
import de.qabel.core.repositories.AndroidVersionAdapter
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.storage.server.BlockServer
import java.io.File
import java.sql.Connection

object JdbcPrefix {
    @JvmField
    var jdbcPrefix = "jdbc:sqldroid:"
}

private fun keysAndVolume(volumeRoot: VolumeRoot,
                          identityRepository: IdentityRepository,
                          deviceId: ByteArray,
                          tempDir: File,
                          androidBlockServer: BlockServer,
                          localStorage: LocalStorage): Pair<BoxReadFileBrowser.KeyAndPrefix, VolumeNavigator> {
    val docId = volumeRoot.documentID.toDocumentId()
    val key = docId.identityKey
    val identity = identityRepository.find(key)
    val backend = BoxHttpStorageBackend(androidBlockServer, docId.prefix)
    val dataBaseFactory: (Connection) -> DirectoryMetadataDatabase = { connection ->
        DirectoryMetadataDatabase(connection, AndroidVersionAdapter(connection))
    }
    //TODO PrefixChooser
    val prefix = identity.prefixes.find { it.prefix == docId.prefix } ?: throw QblStorageException("Prefix not found!")
    val prefixKey = prefix.prefix
    val keyAndPrefix = BoxReadFileBrowser.KeyAndPrefix(identity.keyIdentifier, prefixKey)

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
    return Pair(keyAndPrefix, BoxVolumeNavigator(keyAndPrefix, volume, localStorage))
}


fun makeFileBrowserFactory(identityRepository: IdentityRepository,
                           contactRepository: ContactRepository,
                           deviceId: ByteArray,
                           tempDir: File,
                           androidBlockServer: BlockServer, schedulers: BoxSchedulers,
                           localStorage: LocalStorage):
        Pair<(VolumeRoot) -> ReadFileBrowser, (VolumeRoot) -> OperationFileBrowser> {

    return Pair(
            fun(volumeRoot: VolumeRoot): ReadFileBrowser {
                val (keyAndPrefix, volumeNavigator) = keysAndVolume(volumeRoot, identityRepository,
                        deviceId, tempDir, androidBlockServer, localStorage)
                return BoxReadFileBrowser(keyAndPrefix, volumeNavigator, contactRepository, schedulers)
            },
            fun(volumeRoot: VolumeRoot): OperationFileBrowser {
                val (keyAndPrefix, volumeNavigator) = keysAndVolume(volumeRoot, identityRepository,
                        deviceId, tempDir, androidBlockServer, localStorage)
                return BoxOperationFileBrowser(keyAndPrefix, volumeNavigator, contactRepository,
                        localStorage, schedulers)
            })
}

