package de.qabel.qabelbox.dagger.modules;

import android.support.v7.app.AppCompatActivity;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.communication.connection.ConnectivityManager;

@Module
public class ActivityModule {

    private final AppCompatActivity activity;

    public ActivityModule(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Provides AppCompatActivity provideActivity() {
        return this.activity;
    }

    @Provides ConnectivityManager provideConnectivityManager() {
        return new ConnectivityManager(activity);

    }

}
