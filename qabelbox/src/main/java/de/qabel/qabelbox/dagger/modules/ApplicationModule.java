package de.qabel.qabelbox.dagger.modules;

import android.content.Context;
import android.support.v7.app.NotificationCompat;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.internal.Factory;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.chat.AndroidChatNotificationPresenter;
import de.qabel.qabelbox.chat.ChatNotificationManager;
import de.qabel.qabelbox.chat.ChatNotificationPresenter;
import de.qabel.qabelbox.chat.SyncAdapterChatNotificationManager;
import de.qabel.qabelbox.services.DropConnector;
import de.qabel.qabelbox.services.HttpDropConnector;

@Module
public class ApplicationModule extends ContextModule {

    public ApplicationModule(QabelBoxApplication application) {
        super(application);
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

    @Provides
    Factory<NotificationCompat.Builder> providesNotificationBuilder(
            final Context context) {
        return new Factory<NotificationCompat.Builder>() {
            @Override
            public NotificationCompat.Builder get() {
                return new NotificationCompat.Builder(context);
            }
        };
    }

}
