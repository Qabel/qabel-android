package de.qabel.qabelbox.dagger.components;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import de.qabel.qabelbox.dagger.modules.ActivityModule;
import de.qabel.qabelbox.dagger.modules.ApplicationModule;
import de.qabel.qabelbox.dagger.modules.RepositoryModule;

@Component(modules = {ApplicationModule.class, RepositoryModule.class})
@Singleton
public interface ApplicationComponent {
    Context context();

    ActivityComponent plus(ActivityModule activityModule);
}
