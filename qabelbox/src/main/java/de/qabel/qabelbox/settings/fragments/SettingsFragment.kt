package de.qabel.qabelbox.settings.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment

import org.apache.commons.io.FileUtils

import javax.inject.Inject

import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.account.AccountManager
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.helper.UIHelper
import de.qabel.qabelbox.index.ContactSyncAdapter
import de.qabel.qabelbox.index.preferences.AndroidIndexPreferences
import de.qabel.qabelbox.index.preferences.IndexPreferences
import de.qabel.qabelbox.settings.SettingsActivity
import de.qabel.qabelbox.settings.navigation.SettingsNavigator
import de.qabel.qabelbox.storage.model.BoxQuota

class SettingsFragment : PreferenceFragment() {

    companion object {
        @JvmField
        val APP_PREF_NAME = AndroidIndexPreferences.KEY
    }

    @Inject
    internal lateinit var accountManager: AccountManager
    @Inject
    internal lateinit var appPreferences: AppPreference
    @Inject
    internal lateinit var indexPreferences: IndexPreferences
    @Inject
    internal lateinit var settingsNavigator: SettingsNavigator

    private val accountBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshAccountData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define the settings file to use by this settings fragment
        preferenceManager.sharedPreferencesName = APP_PREF_NAME

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.app_settings)
        findPreference(getString(R.string.setting_change_account_password)).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            settingsNavigator.selectChangeAccountPasswordFragment()
            true
        }

        findPreference(getString(R.string.setting_internal_feedback)).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            settingsNavigator.showFeedbackActivity()
            true
        }
        findPreference(getString(R.string.setting_logout)).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            UIHelper.showConfirmationDialog(this@SettingsFragment.activity, R.string.logout,
                    R.string.logout_confirmation, R.drawable.account_off
            ) { dialog, which ->
                accountManager.logout()
                this@SettingsFragment.activity.finish()
            }
            true
        }

        val contactSync = findPreference(AndroidIndexPreferences.CONTACT_SYNC)
        contactSync.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val enabled = newValue is Boolean && newValue === true
            if (enabled != indexPreferences.contactSyncEnabled) {
                indexPreferences.contactSyncEnabled = enabled
                ContactSyncAdapter.Manager.configureSync(activity)
                if (enabled) {
                    ContactSyncAdapter.Manager.startOnDemandSyncAdapter()
                }
            }
            true
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as SettingsActivity).component.inject(this)

        (findPreference(AndroidIndexPreferences.CONTACT_SYNC) as CheckBoxPreference)
                .isChecked = indexPreferences.contactSyncEnabled
        refreshAccountData()
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(accountBroadcastReceiver,
                IntentFilter(QblBroadcastConstants.Account.ACCOUNT_CHANGED))
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(accountBroadcastReceiver)
    }

    private fun refreshAccountData() {
        val accountName = appPreferences.accountName
        val email = appPreferences.accountEMail

        val accountPref = findPreference(getString(R.string.setting_account_name))
        accountPref.title = getString(R.string.loggedInAs, accountName)
        accountPref.summary = email


        val quota = accountManager.boxQuota
        val usedStorage = FileUtils.byteCountToDisplaySize(quota.size)

        val summaryLabel = if (quota.quota < 0)
            getString(R.string.currently_not_available)
        else if (quota.quota > 0)
            getString(R.string.used_storage, usedStorage,
                    FileUtils.byteCountToDisplaySize(quota.quota))
        else
            getString(R.string.unlimited_storage, usedStorage)


        findPreference(getString(R.string.setting_box_quota)).summary = summaryLabel
    }

}

