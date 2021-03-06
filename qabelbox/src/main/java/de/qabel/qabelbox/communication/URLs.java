package de.qabel.qabelbox.communication;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;

public class URLs {

    private static String BASE_ACCOUNTING;
    private static String BASE_BLOCK;

    public URLs(Context context) {

        if (BASE_ACCOUNTING == null) {
            BASE_ACCOUNTING = context.getString(R.string.accountingServer);
        }
        if (BASE_BLOCK == null) {
            BASE_BLOCK = context.getString(R.string.blockServer);
        }
    }

    @Deprecated
    public URLs() {
        this(QabelBoxApplication.getInstance().getApplicationContext());
    }

    public static void setBaseAccountingURL(String url) {
        BASE_ACCOUNTING = url;
    }

    public static void setBaseBlockURL(String url) {
        BASE_BLOCK = url;
    }

    public String getBaseAccounting() {
        return BASE_ACCOUNTING;
    }

    public String getBaseBlock() {
        return BASE_BLOCK;
    }

    //
    //accounting server
    //
    public String getRegister() {

        return BASE_ACCOUNTING + "/api/v0/auth/registration/";
    }

    public String getLogin() {

        return BASE_ACCOUNTING + "/api/v0/auth/login/";
    }

    public String getLogout() {

        return BASE_ACCOUNTING + "/api/v0/auth/logout/";
    }

    public String getPasswordChange() {

        return BASE_ACCOUNTING + "/api/v0/auth/password/change/";
    }

    public String getPasswordReset() {

        return BASE_ACCOUNTING + "/api/v0/auth/password/reset/";
    }

    //prefix server
    //
    public String getPrefix() {

        return BASE_BLOCK + "/api/v0/prefix/";
    }

    /**
     * get files url for block server
     *
     * @return
     */
    public String getFiles() {

        return BASE_BLOCK + "/api/v0/files/";
    }

    /**
     * get files url for block server
     *
     * @return
     */
    public String getFilesBlock() {

        return BASE_BLOCK + "/api/v0/";
    }
}
