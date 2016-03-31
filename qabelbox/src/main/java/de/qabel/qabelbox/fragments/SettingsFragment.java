package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import de.qabel.qabelbox.R;
import net.hockeyapp.android.FeedbackManager;

public class SettingsFragment extends PreferenceFragment {

    public static final String APP_PREF_NAME = "appsettings";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // Define the settings file to use by this settings fragment
        getPreferenceManager().setSharedPreferencesName(APP_PREF_NAME);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.app_settings);
        findPreference(getString(R.string.settings_key_change_password)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                getFragmentManager().beginTransaction().replace(R.id.fragment_container_content, new ChangeBoxAccountPasswordFragment()).addToBackStack(null).commit();
                return true;
            }
        });
        //@todo only for debug. remove in production version
        findPreference(getString(R.string.settings_key_internal_feedback)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                FeedbackManager.register(getActivity(), getString(R.string.hockeykey), null);
                FeedbackManager.showFeedbackActivity(getActivity());
                return true;
            }
        });
    }
}

