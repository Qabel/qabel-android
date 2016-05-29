package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.providers.DocumentIdParser;
import de.qabel.qabelbox.storage.AndroidBoxManager;
import de.qabel.qabelbox.storage.BoxManager;
import de.qabel.qabelbox.storage.notifications.AndroidStorageNotificationManager;
import de.qabel.qabelbox.storage.notifications.AndroidStorageNotificationPresenter;
import de.qabel.qabelbox.storage.notifications.StorageNotificationManager;
import de.qabel.qabelbox.storage.transfer.BlockServerTransferManager;
import de.qabel.qabelbox.storage.transfer.TransferManager;

@Module
public class StorageModule {

    @Singleton @Provides
    StorageNotificationManager providesStorageNotificationManager(Context context){
        return new AndroidStorageNotificationManager(new AndroidStorageNotificationPresenter(context));
    }

    @Singleton @Provides
    DocumentIdParser providesDocumentIdParser(){
        return new DocumentIdParser();
    }

    @Provides
    File providesCacheDir(Context context){
        return context.getCacheDir();
    }

    @Singleton @Provides
    TransferManager providesTransferManager(Context context, File tmpFile){
        return new BlockServerTransferManager(context, tmpFile);
    }

    @Singleton @Provides BoxManager providesBoxManager(Context context,
                                                       StorageNotificationManager notificationManager,
                                                       DocumentIdParser documentIdParser,
                                                       AppPreference preferences,
                                                       TransferManager transferManager,
                                                       IdentityRepository identityRepository){
        return new AndroidBoxManager(context, notificationManager,
                documentIdParser, preferences, transferManager, identityRepository);
    }

}
