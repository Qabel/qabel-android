package de.qabel.android.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;

import java.util.Date;

import de.qabel.android.BuildConfig;
import de.qabel.android.R;
import de.qabel.android.activities.MainActivity;
import de.qabel.android.helper.Formatter;

/**
 * Created by danny on 25.01.2016.
 */
public class HelpMainFragment extends PreferenceFragment {

    final public static String APP_PREF_NAME = "appsettings";
    private MainActivity mActivity;

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        getFragmentManager().addOnBackStackChangedListener(backstackListener);
        if (activity instanceof MainActivity) {
            mActivity = (MainActivity) activity;
        } else {
            new Throwable("Can't attach to activity. Need mainactivty");
        }
    }

    @Override
    public void onDetach() {

        getFragmentManager().removeOnBackStackChangedListener(backstackListener);
        super.onDetach();
    }

    @Override
    public void onStart() {

        super.onStart();
        setTitleAndActionbar();
    }

    protected void setTitleAndActionbar() {

        mActivity.getSupportActionBar().setTitle(R.string.headline_main_help);
        mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        mActivity.toggle.setDrawerIndicatorEnabled(true);
        mActivity.fab.hide();
    }

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
        setClickListener(findPreference(getString(R.string.help_key_main_app_data_policy)), WebViewHelpFragment.MODE_DATA_POLICY);
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

        PackageManager manager = getActivity().getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(
                    getActivity().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String version = info.versionName;
        return getString(R.string.app_info_version_and_build).replace("$1", info.versionName).replace("$2", getBuildDate());
    }

    String getBuildDate() {

        return Formatter.formatDateShort(new Date(BuildConfig.TIMESTAMP));
    }

    FragmentManager.OnBackStackChangedListener backstackListener = new FragmentManager.OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            // Set FAB visibility according to currently visible fragment
            Fragment activeFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            if (activeFragment instanceof HelpMainFragment) {
                setTitleAndActionbar();
            }
        }
    };
}

