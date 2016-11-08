package de.qabel.qabelbox.dagger.components;

import android.support.v7.app.AppCompatActivity;

import dagger.Subcomponent;
import de.qabel.qabelbox.dagger.modules.ActiveIdentityModule;
import de.qabel.qabelbox.dagger.modules.ActivityModule;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.settings.dagger.SettingsActivityComponent;
import de.qabel.qabelbox.settings.dagger.SettingsActivityModule;

@ActivityScope
@Subcomponent(
        modules = ActivityModule.class
)
public interface ActivityComponent {
    ActiveIdentityComponent plus(ActiveIdentityModule activeIdentityModule);
    SettingsActivityComponent plus(SettingsActivityModule settingsActivityModule);

    AppCompatActivity activity();

}


