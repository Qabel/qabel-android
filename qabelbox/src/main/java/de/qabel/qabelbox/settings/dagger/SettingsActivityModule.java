package de.qabel.qabelbox.settings.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.settings.SettingsActivity;
import de.qabel.qabelbox.settings.navigation.SettingsNavigator;
import de.qabel.qabelbox.settings.navigation.SettingsNavigatorImpl;

@Module
@ActivityScope
public class SettingsActivityModule {

    private final SettingsActivity settingsActivity;

    public SettingsActivityModule(SettingsActivity activity) {
        this.settingsActivity = activity;
    }

    @Provides
    public SettingsActivity providesSettingsActivity(){
        return settingsActivity;
    }

    @Provides
    public SettingsNavigator providesSettingsNavigator() {
        return new SettingsNavigatorImpl(settingsActivity);
    }
}
