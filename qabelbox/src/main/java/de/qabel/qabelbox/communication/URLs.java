package de.qabel.qabelbox.communication;

import android.content.Context;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;

/**
 * Created by danny on 26.01.2016.
 */
public class URLs {

    private String BASE_ACCOUNTING;
    private String BASE_BLOCK;

    public URLs() {

        Context context = QabelBoxApplication.getInstance().getApplicationContext();
        BASE_ACCOUNTING = context.getString(R.string.blockServer);
        BASE_BLOCK = context.getString(R.string.accountingServer);
    }

    public void setBaseAccountingURL(String url) {

        BASE_ACCOUNTING = url;
    }

    public void setBaseBlockURL(String url) {

        BASE_BLOCK = url;
    }

    //
    //accounting server
    //
    String getRegister() {

        return BASE_ACCOUNTING + "/api/v0/auth/registration/";
    }

    String getLogin() {

        return BASE_ACCOUNTING + "/api/v0/auth/login/";
    }

    String getLogout() {

        return BASE_ACCOUNTING + "/api/v0/auth/logout/";
    }

    String getPasswordChange() {

        return BASE_ACCOUNTING + "/api/v0/auth/password/change/";
    }

    String getPasswordReset() {

        return BASE_ACCOUNTING + "/api/v0/auth/password/reset/";
    }

    //prefix server
    //
    String getPrefix() {

        return BASE_ACCOUNTING + "/api/v0/prefix/";
    }

    /**
     * get files url for block server
     *
     * @return
     */
    String getFiles() {

        return BASE_BLOCK + "/api/v0/files/";
    }

    /**
     * get files url for block server
     *
     * @return
     */
    String getFilesBlock() {

        return BASE_BLOCK + "/api/v0/";
    }
}
