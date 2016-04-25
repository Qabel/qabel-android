package de.qabel.qabelbox.helper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

public class AccountHelper {

    private static final String ACCOUNT = "sync";
    private static final String ACCOUNT_TYPE = "de.qabel";

    public static void createSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager = AccountManager.get(context);
        accountManager.addAccountExplicitly(newAccount, null, null);
    }

}
