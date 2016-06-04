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
import de.qabel.qabelbox.storage.model.BoxQuota;
import de.qabel.qabelbox.storage.server.BlockServer;
import okhttp3.Response;

public class AccountManager {

    private static final String TAG = AccountManager.class.getSimpleName();
    private static final String VERSION_KEY = "accountVersion";

    private Context context;
    private AppPreference preferences;
    private BlockServer blockServer;
    private BoxAccountRegisterServer accountRegisterServer;

    private long version = 0;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.hasExtra(VERSION_KEY) || intent.getLongExtra(VERSION_KEY, 0) != version) {
                refreshQuota();
            }
        }
    };


    public AccountManager(Context context, AppPreference preference, BlockServer blockServer, BoxAccountRegisterServer registerServer) {
        this.context = context;
        this.preferences = preference;
        this.blockServer = blockServer;
        this.accountRegisterServer = registerServer;
        context.registerReceiver(broadcastReceiver,
                new IntentFilter(QblBroadcastConstants.Account.ACCOUNT_CHANGED));
    }

    public void refreshQuota() {
        blockServer.getQuota(new JSONModelCallback<BoxQuota>() {
            @Override
            protected BoxQuota createModel() {
                return new BoxQuota();
            }

            @Override
            protected void onSuccess(Response response, BoxQuota model) {
                preferences.setBoxQuota(model);
                version++;
                broadcastAccountChanged(version);
            }

            @Override
            protected void onError(Exception e, @Nullable Response response) {
                Log.e(TAG, "Cannot receive quota from blockserver");
            }
        });
    }

    private void broadcastAccountChanged(long version) {
        System.out.println("SEND BROADCAST");
        Intent intent = new Intent(QblBroadcastConstants.Account.ACCOUNT_CHANGED);
        intent.putExtra(VERSION_KEY, version);
        context.sendBroadcast(intent);
    }

    public BoxQuota getBoxQuota(){
        BoxQuota quota = preferences.getBoxQuota();
        if(quota.getQuota() < 0){
            refreshQuota();
        }
        return quota;
    }
}
