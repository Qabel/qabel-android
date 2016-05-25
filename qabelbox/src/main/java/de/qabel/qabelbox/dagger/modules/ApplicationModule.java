package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.chat.ChatServer;

@Module
public class ApplicationModule {

    public ApplicationModule(QabelBoxApplication application) {
        this.application = application;
    }

    private final QabelBoxApplication application;

    @Provides @Singleton public Context provideApplicationContext() {
        return application;
    }

    @Provides
    ChatServer provideChatServer(Context context) {
        return new ChatServer(context);
    }

}
