package de.qabel.qabelbox.account;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.util.Log;

import de.qabel.qabelbox.QblBroadcastConstants;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.communication.callbacks.JSONModelCallback;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.storage.data.BoxQuotaJSONAdapter;
import de.qabel.qabelbox.storage.model.BoxQuota;
import de.qabel.qabelbox.storage.server.BlockServer;
import okhttp3.Response;

public class AccountManager {

    private static final String TAG = AccountManager.class.getSimpleName();

    private Context context;
    private AppPreference preferences;
    private BlockServer blockServer;
    private BoxAccountRegisterServer accountRegisterServer;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshQuota();
        }
    };


    public AccountManager(Context context, AppPreference preference, BlockServer blockServer, BoxAccountRegisterServer registerServer) {
        this.context = context;
        this.preferences = preference;
        this.blockServer = blockServer;
        this.accountRegisterServer = registerServer;
        context.registerReceiver(broadcastReceiver,
                new IntentFilter(QblBroadcastConstants.Storage.BOX_CHANGED));
    }

    public void refreshQuota() {
        blockServer.getQuota(new JSONModelCallback<BoxQuota>(
                new BoxQuotaJSONAdapter()) {

            @Override
            protected void onSuccess(Response response, BoxQuota model) {
                preferences.setBoxQuota(model);
                broadcastAccountChanged(AccountStatusCodes.QUOTA_UPDATED);
            }

            @Override
            protected void onError(Exception e, @Nullable Response response) {
                Log.e(TAG, "Cannot receive quota from blockserver");
            }
        });
    }

    private void broadcastAccountChanged(int statusCode) {
        Intent intent = new Intent(QblBroadcastConstants.Account.ACCOUNT_CHANGED);
        intent.putExtra(QblBroadcastConstants.STATUS_CODE_PARAM, statusCode);
        context.sendBroadcast(intent);
    }

    public BoxQuota getBoxQuota() {
        BoxQuota quota = preferences.getBoxQuota();
        if (quota.getQuota() < 0) {
            refreshQuota();
        }
        return quota;
    }

    public void logout(){
        preferences.setToken(null);
        preferences.setAccountName(null);
        preferences.setAccountEMail(null);
        broadcastAccountChanged(AccountStatusCodes.LOGOUT);
    }
}
