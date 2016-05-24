package de.qabel.qabelbox.dagger.components;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import de.qabel.qabelbox.activities.BaseActivity;
import de.qabel.qabelbox.dagger.modules.ActivityModule;
import de.qabel.qabelbox.dagger.modules.ApplicationModule;
import de.qabel.qabelbox.dagger.modules.IdentityModule;
import de.qabel.qabelbox.dagger.modules.RepositoryModule;
import de.qabel.qabelbox.fragments.BaseFragment;

@Component(modules = {ApplicationModule.class, RepositoryModule.class})
@Singleton
public interface ApplicationComponent {
    void inject(BaseActivity baseActivity);
    void inject(BaseFragment baseFragment);

    Context context();

    ActivityComponent plus(ActivityModule activityModule);
    IdentityComponent plus(IdentityModule identityModule);
}
