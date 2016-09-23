package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.chat.repository.ChatShareRepository;
import de.qabel.chat.service.MainSharingService;
import de.qabel.chat.service.SharingService;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.repository.ContactRepository;
import de.qabel.qabelbox.box.provider.DocumentIdParser;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.storage.notifications.AndroidStorageNotificationManager;
import de.qabel.qabelbox.storage.notifications.AndroidStorageNotificationPresenter;
import de.qabel.qabelbox.storage.notifications.StorageNotificationManager;
import de.qabel.qabelbox.storage.server.AndroidBlockServer;
import de.qabel.qabelbox.storage.server.BlockServer;

@Module
public class StorageModule {

    @Singleton
    @Provides
    StorageNotificationManager providesStorageNotificationManager(Context context) {
        return new AndroidStorageNotificationManager(new AndroidStorageNotificationPresenter(context));
    }

    @Singleton
    @Provides
    DocumentIdParser providesDocumentIdParser() {
        return new DocumentIdParser();
    }

    @Provides
    File providesCacheDir(Context context) {
        return context.getCacheDir();
    }

    @Singleton
    @Provides
    BlockServer providesBlockServer(AppPreference preference, Context context) {
        return createBlockServer(preference, context);
    }

    protected BlockServer createBlockServer(AppPreference preference, Context context){
        return new AndroidBlockServer(preference, context);
    }

    @Singleton
    @Provides
    SharingService providesSharingService(ChatShareRepository shareRepository, ContactRepository contactRepository,
                                          Context context) {
        return new MainSharingService(shareRepository, contactRepository, context.getCacheDir(), new CryptoUtils());
    }

}
