package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import java.io.File;
import java.sql.Connection;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.box.storage.FileMetadataFactory;
import de.qabel.box.storage.jdbc.JdbcFileMetadataFactory;
import de.qabel.chat.repository.ChatShareRepository;
import de.qabel.chat.service.MainSharingService;
import de.qabel.chat.service.SharingService;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.repositories.AndroidVersionAdapter;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.sqlite.VersionAdapter;
import de.qabel.qabelbox.box.provider.DocumentIdParser;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.storage.notifications.AndroidStorageNotificationManager;
import de.qabel.qabelbox.storage.notifications.AndroidStorageNotificationPresenter;
import de.qabel.qabelbox.storage.notifications.StorageNotificationManager;
import de.qabel.qabelbox.storage.server.AndroidBlockServer;
import de.qabel.qabelbox.storage.server.BlockServer;
import kotlin.jvm.functions.Function1;

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
    FileMetadataFactory provideFileMetadataFactory(File cacheDir) {
        return new JdbcFileMetadataFactory(cacheDir, new Function1<Connection, VersionAdapter>() {
            @Override
            public VersionAdapter invoke(Connection connection) {
                return new AndroidVersionAdapter(connection);
            }
        });
    }

    @Singleton
    @Provides
    SharingService providesSharingService(ChatShareRepository shareRepository,
                                          ContactRepository contactRepository,
                                          File cacheDir,
                                          FileMetadataFactory fileMetadataFactory) {
        return new MainSharingService(
                shareRepository,
                contactRepository,
                cacheDir,
                fileMetadataFactory,
                new CryptoUtils());
    }

}
