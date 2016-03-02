package de.qabel.qabelbox.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import net.hockeyapp.android.FeedbackManager;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 25.01.2016.
 */
public class SettingsFragment extends PreferenceFragment {

    final public static String APP_PREF_NAME = "appsettings";

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
		findPreference(getString(R.string.settings_key_internal_reset_app_data)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				clearAllAppData(getActivity());
				return true;
			}
		});

    }
	public static void clearAllAppData(Context context) {
		if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
			boolean ret=	((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
					.clearApplicationUserData(); // note: it has a return value!
		} else {
			// once minSdkVersion goes above 19 in a few years.
		}
	}
}

