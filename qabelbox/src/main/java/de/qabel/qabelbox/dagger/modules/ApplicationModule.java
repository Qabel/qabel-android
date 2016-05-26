package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.chat.AndroidChatNotificationPresenter;
import de.qabel.qabelbox.chat.ChatNotificationManager;
import de.qabel.qabelbox.chat.ChatNotificationPresenter;
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

    @Singleton @Provides DropConnector providesDropConnector(HttpDropConnector connector) {
        return connector;
    }

    @Singleton @Provides ChatNotificationManager providesChatNotificationManager(
            SyncAdapterChatNotificationManager manager) {
        return manager;
    }

    @Singleton @Provides ChatNotificationPresenter providesChatNotificationPresenter(
            AndroidChatNotificationPresenter presenter) {
        return presenter;
    }
}
