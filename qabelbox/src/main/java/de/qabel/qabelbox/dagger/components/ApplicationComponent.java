package de.qabel.qabelbox.dagger.components;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import de.qabel.qabelbox.adapter.QabelSyncAdapter;
import de.qabel.qabelbox.dagger.modules.AccountModule;
import de.qabel.qabelbox.dagger.modules.ActivityModule;
import de.qabel.qabelbox.dagger.modules.ApplicationModule;
import de.qabel.qabelbox.dagger.modules.RepositoryModule;
import de.qabel.qabelbox.dagger.modules.StorageModule;
import de.qabel.qabelbox.settings.fragments.SettingsFragment;

@Component(modules = {ApplicationModule.class, RepositoryModule.class, AccountModule.class, StorageModule.class})
@Singleton
public interface ApplicationComponent {
    Context context();

    ActivityComponent plus(ActivityModule activityModule);

    void inject(QabelSyncAdapter syncAdapter);

}
