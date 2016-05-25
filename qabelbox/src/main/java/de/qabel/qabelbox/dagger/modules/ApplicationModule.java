package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.chat.ChatNotificationManager;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.chat.SyncAdapterChatNotificationManager;
import de.qabel.qabelbox.services.DropConnector;
import de.qabel.qabelbox.services.HttpDropConnector;

@Module
public class ApplicationModule {

    public ApplicationModule(QabelBoxApplication application) {
        this.application = application;
    }

    private final QabelBoxApplication application;

    @Provides @Singleton public Context provideApplicationContext() {
        return application;
    }

    @Provides DropConnector providesDropConnector(HttpDropConnector connector) {
        return connector;
    }

    @Provides ChatNotificationManager providesChatNotificationManager(
            SyncAdapterChatNotificationManager manager) {
        return manager;
    }
}
