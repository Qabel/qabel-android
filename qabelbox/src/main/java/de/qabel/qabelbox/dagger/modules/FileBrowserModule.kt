package de.qabel.qabelbox.dagger.modules

import dagger.Module
import dagger.Provides
import de.qabel.box.storage.StorageReadBackend
import de.qabel.box.storage.StorageWriteBackend
import de.qabel.core.config.Identity
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.box.interactor.*
import de.qabel.qabelbox.box.presenters.FileBrowserPresenter
import de.qabel.qabelbox.box.presenters.MainFileBrowserPresenter
import de.qabel.qabelbox.box.provider.DocumentIdParser
import de.qabel.qabelbox.box.views.FileBrowserView
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.dagger.scopes.ActivityScope
import de.qabel.qabelbox.storage.server.BlockServer

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
        return BoxHttpStorageBackend(blockServer, identity.prefixes.first().prefix)
    }

    @ActivityScope
    @Provides
    fun provideNavigator(browser: ReadFileBrowser): VolumeNavigator {
        //TODO Really bad
        return (browser as BoxReadFileBrowser).volumeNavigator
    }

    @ActivityScope
    @Provides
    fun provideFileBrowserUseCase(documentIdParser: DocumentIdParser,
                                  keyAndPrefix: BoxReadFileBrowser.KeyAndPrefix,
                                  volumeManager: VolumeManager): ReadFileBrowser {
        return volumeManager.readFileBrowser(documentIdParser.buildId(keyAndPrefix.publicKey, keyAndPrefix.prefix))
    }

    @ActivityScope
    @Provides
    fun provideKeyAndPrefix(identity: Identity): BoxReadFileBrowser.KeyAndPrefix {
        return BoxReadFileBrowser.KeyAndPrefix(identity)
    }

}
