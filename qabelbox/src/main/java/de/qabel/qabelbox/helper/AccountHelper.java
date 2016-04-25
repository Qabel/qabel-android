package de.qabel.qabelbox.helper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import de.qabel.qabelbox.BuildConfig;

public class AccountHelper {

    private static final String ACCOUNT = "sync";
    private static final String ACCOUNT_TYPE = "de.qabel";
    public static final Account DEFAULT_ACCOUNT = new Account(ACCOUNT, ACCOUNT_TYPE);
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".providers.internal";
    private static final long SYNC_INTERVAL = 1;

    public static void createSyncAccount(Context context) {
        // Create the account type and default account
        // Get an instance of the Android account manager
        AccountManager accountManager = AccountManager.get(context);
        accountManager.addAccountExplicitly(DEFAULT_ACCOUNT, null, null);
    }

    public static void startOnDemandSyncAdapter(Context context) {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(DEFAULT_ACCOUNT, AUTHORITY, settingsBundle);
    }

    public static void configurePeriodicPolling() {
        Log.d("AccountHelper", "Set periodic polling");
        ContentResolver.addPeriodicSync(
                DEFAULT_ACCOUNT,
                AUTHORITY,
                Bundle.EMPTY,
                SYNC_INTERVAL);

    }
}
