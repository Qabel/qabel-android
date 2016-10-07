package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.account.AccountManager;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.storage.server.BlockServer;

@Module
public class AccountModule {

    @Singleton
    @Provides
    BoxAccountRegisterServer providesAccountServer(AppPreference preference, Context context) {
        return new BoxAccountRegisterServer(context, preference);
    }

    @Singleton
    @Provides
    AccountManager providesAccountManager(Context context, AppPreference preference, BlockServer blockServer) {
        return new AccountManager(context, preference, blockServer);
    }
}
