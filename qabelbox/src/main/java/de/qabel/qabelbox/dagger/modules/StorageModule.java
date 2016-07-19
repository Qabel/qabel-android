package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.box.dto.VolumeRoot;
import de.qabel.qabelbox.box.interactor.BoxProviderUseCase;
import de.qabel.qabelbox.box.interactor.FileBrowserUseCase;
import de.qabel.qabelbox.box.interactor.MockFileBrowserUseCase;
import de.qabel.qabelbox.box.interactor.ProviderUseCase;
import de.qabel.qabelbox.box.interactor.VolumeManager;
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

}
