package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 25.01.2016.
 */
public class HelpMainFragment extends PreferenceFragment {

    final public static String APP_PREF_NAME = "appsettings";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Define the settings file to use by this settings fragment
        getPreferenceManager().setSharedPreferencesName(APP_PREF_NAME);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.app_main_help);

        //set app version infos
        Preference appVersion = findPreference(getString(R.string.help_key_main_app_version));
        appVersion.setSummary(getAppInfos());

        //set click listener for all other entrys
        setClickListener(findPreference(getString(R.string.help_key_main_app_disclaimer)), WebViewHelpFragment.MODE_DISCLAIMER);
        setClickListener(findPreference(getString(R.string.help_key_main_app_data_policy)), WebViewHelpFragment.MODE_DATA_POLICY);
        setClickListener(findPreference(getString(R.string.help_key_main_app_licenses)), WebViewHelpFragment.MODE_LICENSES);
        setClickListener(findPreference(getString(R.string.help_key_main_app_tou)), WebViewHelpFragment.MODE_TOU);
        setClickListener(findPreference(getString(R.string.help_key_main_app_about_us)), WebViewHelpFragment.MODE_ABOUT_US);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
    }
    private void setClickListener(Preference preference, final int mode) {

        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                getFragmentManager().beginTransaction().add(R.id.fragment_container, WebViewHelpFragment.newInstance(mode), null).addToBackStack(null).commit();
                return false;
            }
        });
    }

    public String getAppInfos() {

        return "Versi";
    }
}

