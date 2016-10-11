package de.qabel.qabelbox.dagger.modules

import dagger.Module
import dagger.Provides
import de.qabel.box.storage.*
import de.qabel.box.storage.jdbc.DirectoryMetadataDatabase
import de.qabel.box.storage.jdbc.JdbcDirectoryMetadataFactory
import de.qabel.box.storage.jdbc.JdbcFileMetadataFactory
import de.qabel.core.config.Identity
import de.qabel.core.repositories.AndroidVersionAdapter
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.box.interactor.*
import de.qabel.qabelbox.box.presenters.FileBrowserPresenter
import de.qabel.qabelbox.box.presenters.MainFileBrowserPresenter
import de.qabel.qabelbox.box.views.FileBrowserView
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.dagger.scopes.ActivityScope
import de.qabel.qabelbox.storage.server.BlockServer
import java.io.File
import java.sql.Connection

@ActivityScope
@Module
class FileBrowserModule(private val view: FileBrowserView) {

    @ActivityScope
    @Provides
    fun provideFileBrowserView(): FileBrowserView {
        return view
    }

    @ActivityScope
    @Provides
    fun providerFileBrowserPresenter(mainFileBrowserPresenter: MainFileBrowserPresenter)
            : FileBrowserPresenter {
        return mainFileBrowserPresenter
    }

    @ActivityScope
    @Provides
    fun provideSharer(boxSharer: BoxSharer): Sharer {
       return boxSharer
    }

    @ActivityScope
    @Provides
    fun provideDeviceId(appPreference: AppPreference): ByteArray {
        return appPreference.deviceId
    }


    @ActivityScope
    @Provides
    fun provideReadBackend(boxHttpStorageBackend: BoxHttpStorageBackend): StorageReadBackend {
        return boxHttpStorageBackend
    }

    @ActivityScope
    @Provides
    fun provideWriteBackend(boxHttpStorageBackend: BoxHttpStorageBackend): StorageWriteBackend {
        return boxHttpStorageBackend
    }

    @ActivityScope
    @Provides
    fun provideStorageBackend(blockServer: BlockServer, identity: Identity): BoxHttpStorageBackend {
        return BoxHttpStorageBackend(blockServer, identity.prefixes.first())
    }


    @ActivityScope
    @Provides
    fun provideNavigator(navigator: BoxFileBrowser): VolumeNavigator {
        return navigator
    }

    @ActivityScope
    @Provides
    fun provideFileBrowserUseCase(fileBrowserUseCase: BoxFileBrowser): FileBrowser {
        return fileBrowserUseCase
    }

    @ActivityScope
    @Provides
    fun provideKeyAndPrefix(identity: Identity): BoxFileBrowser.KeyAndPrefix {
        return BoxFileBrowser.KeyAndPrefix(identity.keyIdentifier, identity.prefixes.first())
    }

    @ActivityScope
    @Provides
    fun provideBoxVolume(identity: Identity,
                         readBackend: StorageReadBackend,
                         writeBackend: StorageWriteBackend,
                         deviceId: ByteArray,
                         tempDir: File
                         ): BoxVolume {
        val dataBaseFactory: (Connection) -> DirectoryMetadataDatabase = { connection ->
            DirectoryMetadataDatabase(connection, AndroidVersionAdapter(connection))
        }
        val prefix = identity.prefixes.first()
        return AndroidBoxVolume(BoxVolumeConfig(
                prefix,
                deviceId,
                readBackend,
                writeBackend,
                "Blake2b",
                tempDir,
                directoryMetadataFactoryFactory = { tempDir, deviceId ->
                    JdbcDirectoryMetadataFactory(tempDir, deviceId, dataBaseFactory,
                            jdbcPrefix = jdbcPrefix)
                },
                fileMetadataFactoryFactory = { tempDir ->
                    JdbcFileMetadataFactory(tempDir, versionAdapterFactory = { connection ->
                        AndroidVersionAdapter(connection)},
                            jdbcPrefix = jdbcPrefix)
                }),
                identity.primaryKeyPair)

    }

}
