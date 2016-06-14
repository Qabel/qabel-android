package de.qabel.qabelbox.settings.navigation;

import net.hockeyapp.android.FeedbackManager;

import javax.inject.Inject;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.navigation.AbstractNavigator;
import de.qabel.qabelbox.settings.SettingsActivity;
import de.qabel.qabelbox.settings.fragments.ChangeBoxAccountPasswordFragment;
import de.qabel.qabelbox.settings.fragments.SettingsFragment;

public class SettingsNavigatorImpl extends AbstractNavigator
        implements SettingsNavigator {

    private static final String SETTINGS_FRAGMENT = SettingsFragment.class.getSimpleName();
    private static final String CHANGE_ACCOUNT_PW_FRAGMENT = ChangeBoxAccountPasswordFragment.class.getSimpleName();

    @Inject
    SettingsActivity settingsActivity;

    @Inject
    public SettingsNavigatorImpl(SettingsActivity settingsActivity) {
        this.settingsActivity = settingsActivity;
    }

    @Override
    public void selectSettingsFragment() {
        showFragment(settingsActivity, new SettingsFragment(), SETTINGS_FRAGMENT, false, false);
    }

    @Override
    public void selectChangeAccountPasswordFragment() {
        showFragment(settingsActivity, new ChangeBoxAccountPasswordFragment(), CHANGE_ACCOUNT_PW_FRAGMENT, false, false);
    }

    @Override
    public void showFeedbackActivity() {
        FeedbackManager.register(settingsActivity, settingsActivity.
                getString(R.string.hockeykey), null);
        FeedbackManager.showFeedbackActivity(settingsActivity);
    }
}
