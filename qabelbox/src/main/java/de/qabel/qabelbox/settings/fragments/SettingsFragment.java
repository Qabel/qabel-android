package de.qabel.qabelbox.settings.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import org.apache.commons.io.FileUtils;

import javax.inject.Inject;

import de.qabel.qabelbox.QblBroadcastConstants;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.account.AccountManager;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.settings.SettingsActivity;
import de.qabel.qabelbox.settings.navigation.SettingsNavigator;
import de.qabel.qabelbox.storage.model.BoxQuota;

public class SettingsFragment extends PreferenceFragment {

    final public static String APP_PREF_NAME = "settings";

    @Inject
    AccountManager accountManager;
    @Inject
    AppPreference appPreferences;
    @Inject
    SettingsNavigator settingsNavigator;

    private BroadcastReceiver accountBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshAccountData();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Define the settings file to use by this settings fragment
        getPreferenceManager().setSharedPreferencesName(APP_PREF_NAME);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.app_settings);
        findPreference(getString(R.string.setting_change_account_password)).setOnPreferenceClickListener(preference -> {
            settingsNavigator.selectChangeAccountPasswordFragment();
            return true;
        });
        findPreference(getString(R.string.setting_internal_feedback)).setOnPreferenceClickListener(preference -> {
            settingsNavigator.showFeedbackActivity();
            return true;
        });
        findPreference(getString(R.string.setting_logout)).setOnPreferenceClickListener(preference -> {
            UIHelper.showConfirmationDialog(getActivity(), R.string.logout,
                    R.string.logout_confirmation, R.drawable.account_off,
                    (dialog, which) -> {
                        accountManager.logout();
                        getActivity().finish();
                    });
            return true;
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((SettingsActivity)getActivity()).getComponent().inject(this);
        refreshAccountData();
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

    private void refreshAccountData() {
        String accountName = appPreferences.getAccountName();
        String email = appPreferences.getAccountEMail();

        Preference accountPref = findPreference(getString(R.string.setting_account_name));
        accountPref.setTitle(getString(R.string.loggedInAs, accountName));
        accountPref.setSummary(email);


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
        findPreference(getString(R.string.setting_box_quota)).setSummary(summaryLabel);
    }
}

