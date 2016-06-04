package de.qabel.qabelbox.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;

import net.hockeyapp.android.FeedbackManager;

import org.apache.commons.io.FileUtils;

import javax.inject.Inject;

import de.qabel.qabelbox.QblBroadcastConstants;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.account.AccountManager;
import de.qabel.qabelbox.activities.BaseActivity;
import de.qabel.qabelbox.storage.model.BoxQuota;

public class SettingsFragment extends PreferenceFragment {

    final public static String APP_PREF_NAME = "settings";

    @Inject
    AccountManager accountManager;

    private BroadcastReceiver accountBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshQuota();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Define the settings file to use by this settings fragment
        getPreferenceManager().setSharedPreferencesName(APP_PREF_NAME);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.app_settings);
        findPreference(getString(R.string.settings_key_change_password)).setOnPreferenceClickListener(preference -> {
            getFragmentManager().beginTransaction().replace(R.id.fragment_container_content,
                    new ChangeBoxAccountPasswordFragment()).addToBackStack(null).commit();
            return true;
        });

        findPreference(getString(R.string.settings_key_internal_feedback)).setOnPreferenceClickListener(preference -> {
            FeedbackManager.register(getActivity(), getString(R.string.hockeykey), null);
            FeedbackManager.showFeedbackActivity(getActivity());
            return true;
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((BaseActivity)getActivity()).getApplicationComponent().inject(this);
        refreshQuota();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        getActivity().registerReceiver(accountBroadcastReceiver,
                new IntentFilter(QblBroadcastConstants.Account.ACCOUNT_CHANGED));
    }

    @Override
    public void onDetach() {
        getActivity().unregisterReceiver(accountBroadcastReceiver);
        super.onDetach();
    }

    private void refreshQuota() {
        BoxQuota quota = accountManager.getBoxQuota();
        String summaryLabel;
        String usedStorage = FileUtils.byteCountToDisplaySize(quota.getSize());
        if(quota.getQuota() < 0){
            summaryLabel = getString(R.string.currently_not_available);
        }else if (quota.getQuota() > 0) {
            summaryLabel = getString(R.string.used_storage, usedStorage,
                    FileUtils.byteCountToDisplaySize(quota.getQuota()));
        } else {
            summaryLabel = getString(R.string.unlimited_storage, usedStorage);
        }
        findPreference("boxquota").setSummary(summaryLabel);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}

