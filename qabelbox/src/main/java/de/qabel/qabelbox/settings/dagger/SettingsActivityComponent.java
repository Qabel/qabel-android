package de.qabel.qabelbox.settings.dagger;

import dagger.Subcomponent;
import de.qabel.qabelbox.settings.SettingsActivity;
import de.qabel.qabelbox.settings.fragments.ChangeBoxAccountPasswordFragment;
import de.qabel.qabelbox.settings.fragments.SettingsFragment;

@Subcomponent(modules = SettingsActivityModule.class)
public interface SettingsActivityComponent {

    void inject(SettingsActivity settingsActivity);
    void inject(SettingsFragment settingsFragment);

    void inject(ChangeBoxAccountPasswordFragment changeBoxAccountPasswordFragment);
}
