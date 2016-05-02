package de.qabel.qabelbox.helper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.PeriodicSync;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.apache.http.auth.AUTH;

import java.util.List;

import de.qabel.qabelbox.BuildConfig;

public class AccountHelper {

    private static final String ACCOUNT = "sync";
    private static final String ACCOUNT_TYPE = BuildConfig.ACCOUNT_TYPE;
    public static final Account DEFAULT_ACCOUNT = new Account(ACCOUNT, ACCOUNT_TYPE);
    public static final String AUTHORITY = BuildConfig.AUTHORITY;
    private static final long SYNC_INTERVAL = 60;
    private static final String TAG = "AccountHelper";

    public static void createSyncAccount(Context context) {
        // Create the account type and default account
        // Get an instance of the Android account manager
        AccountManager accountManager = AccountManager.get(context);
        accountManager.addAccountExplicitly(DEFAULT_ACCOUNT, null, null);
    }

    public static void startOnDemandSyncAdapter() {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(DEFAULT_ACCOUNT, AUTHORITY, settingsBundle);
    }

    public static void configurePeriodicPolling() {
        Log.i("AccountHelper", "Set periodic polling");
        ContentResolver.setSyncAutomatically(DEFAULT_ACCOUNT, AUTHORITY, true);
        for (PeriodicSync sync: getPeriodicSync()) {
            Log.i(TAG, "Removing sync");
            ContentResolver.removePeriodicSync(sync.account, sync.authority, sync.extras);
        }
        ContentResolver.addPeriodicSync(
                DEFAULT_ACCOUNT,
                AUTHORITY,
                Bundle.EMPTY,
                SYNC_INTERVAL);

    }

    public static List<PeriodicSync> getPeriodicSync() {
        return ContentResolver.getPeriodicSyncs(DEFAULT_ACCOUNT, AUTHORITY);
    }
}
