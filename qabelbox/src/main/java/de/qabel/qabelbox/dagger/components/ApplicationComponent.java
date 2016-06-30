package de.qabel.qabelbox.dagger.components;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.qabelbox.activities.CreateAccountActivity;
import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.activities.SplashActivity;
import de.qabel.qabelbox.adapter.QabelSyncAdapter;
import de.qabel.qabelbox.dagger.modules.AccountModule;
import de.qabel.qabelbox.dagger.modules.ActivityModule;
import de.qabel.qabelbox.dagger.modules.ApplicationModule;
import de.qabel.qabelbox.dagger.modules.RepositoryModule;
import de.qabel.qabelbox.dagger.modules.StorageModule;
import de.qabel.qabelbox.fragments.CreateAccountFinalFragment;
import de.qabel.qabelbox.fragments.CreateIdentityMainFragment;
import de.qabel.qabelbox.settings.fragments.SettingsFragment;

@Component(modules = {ApplicationModule.class, RepositoryModule.class, AccountModule.class, StorageModule.class})
@Singleton
public interface ApplicationComponent {
    Context context();

    IdentityRepository identityRepository();
    ContactRepository contactRepository();

    ActivityComponent plus(ActivityModule activityModule);

    void inject(QabelSyncAdapter syncAdapter);

    void inject(CreateAccountActivity createAccountActivity);

    void inject(CreateIdentityActivity createIdentityActivity);

    void inject(SplashActivity splashActivity);

    void inject(CreateAccountFinalFragment createAccountFinalFragment);

    void inject(CreateIdentityMainFragment createIdentityMainFragment);
}
