package de.qabel.qabelbox.dagger.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import de.qabel.box.storage.FileMetadataFactory
import de.qabel.box.storage.jdbc.JdbcFileMetadataFactory
import de.qabel.chat.repository.ChatShareRepository
import de.qabel.chat.service.MainSharingService
import de.qabel.chat.service.SharingService
import de.qabel.client.box.BoxSchedulers
import de.qabel.client.box.MainBoxSchedulers
import de.qabel.client.box.documentId.DocumentIdParser
import de.qabel.client.box.interactor.BoxVolumeManager
import de.qabel.client.box.interactor.VolumeManager
import de.qabel.client.box.storage.LocalStorage
import de.qabel.core.crypto.CryptoUtils
import de.qabel.core.repositories.AndroidVersionAdapter
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.box.BoxScheduler
import de.qabel.qabelbox.box.interactor.*
import de.qabel.qabelbox.box.notifications.AndroidStorageNotificationManager
import de.qabel.qabelbox.box.notifications.AndroidStorageNotificationPresenter
import de.qabel.qabelbox.box.notifications.StorageNotificationManager
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.storage.server.AndroidBlockServer
import de.qabel.qabelbox.storage.server.BlockServer
import rx.schedulers.Schedulers
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
open class StorageModule {

    @Singleton
    @Provides
    internal fun providesStorageNotificationManager(context: Context): StorageNotificationManager {
        return AndroidStorageNotificationManager(AndroidStorageNotificationPresenter(context))
    }

    @Singleton
    @Provides
    internal fun providesDocumentIdParser(): DocumentIdParser {
        return DocumentIdParser()
    }

    @Provides
    internal fun providesCacheDir(context: Context): File {
        return context.cacheDir
    }

    @Singleton
    @Provides
    internal fun providesBlockServer(preference: AppPreference, context: Context): BlockServer {
        return createBlockServer(preference, context)
    }

    protected open fun createBlockServer(preference: AppPreference, context: Context): BlockServer {
        return AndroidBlockServer(preference, context)
    }

    @Singleton
    @Provides
    internal fun provideFileMetadataFactory(cacheDir: File): FileMetadataFactory {
        return JdbcFileMetadataFactory(cacheDir, ::AndroidVersionAdapter, JdbcPrefix.jdbcPrefix)
    }

    @Singleton
    @Provides
    internal fun providesSharingService(shareRepository: ChatShareRepository,
                                        contactRepository: ContactRepository,
                                        cacheDir: File,
                                        fileMetadataFactory: FileMetadataFactory): SharingService {
        return MainSharingService(
                shareRepository,
                contactRepository,
                cacheDir,
                fileMetadataFactory,
                CryptoUtils())
    }

    @Provides
    @Singleton
    internal fun providesScheduler(): BoxSchedulers {
        return MainBoxSchedulers(Schedulers.from(Executors.newCachedThreadPool()))
    }

    @Singleton
    @Provides
    fun providesDocumentIdUseCase(useCase: BoxDocumentIdInteractor): DocumentIdInteractor {
        return useCase
    }

    @Singleton
    @Provides
    fun provideVolumeManager(identityRepository: IdentityRepository,
                             contactRepository: ContactRepository,
                             preference: AppPreference,
                             context: Context, blockServer: BlockServer,
                             scheduler: BoxSchedulers,
                             localStorage: LocalStorage):
            VolumeManager {
        val (read, operation) = makeFileBrowserFactory(
                identityRepository, contactRepository, preference.deviceId,
                context.cacheDir, blockServer, scheduler, localStorage)
        return BoxVolumeManager(identityRepository, read, operation)
    }

    @Singleton
    @Provides
    fun providesBoxServiceStarter(serviceStarter: AndroidBoxServiceStarter): BoxServiceStarter {
        return serviceStarter
    }

}
