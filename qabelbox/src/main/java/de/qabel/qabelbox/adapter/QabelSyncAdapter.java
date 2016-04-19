package de.qabel.qabelbox.adapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.services.LocalQabelService;

public class QabelSyncAdapter extends AbstractThreadedSyncAdapter {

    ContentResolver mContentResolver;
    Context context;
    LocalQabelService mService;
    boolean resourcesReady = false;

    public QabelSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        init(context);

    }

    public QabelSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        mContentResolver = context.getContentResolver();
        bindToService(context);
    }

    void bindToService(final Context context) {

        Intent intent = new Intent(context, LocalQabelService.class);
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                LocalQabelService.LocalBinder binder = (LocalQabelService.LocalBinder) service;
                if (binder != null) {
                    mService = binder.getService();
                    resourcesReady = true;
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                resourcesReady = false;
                mService = null;
            }
        }, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPerformSync(
            Account account,
		    Bundle extras,
		    String authority,
		    ContentProviderClient provider,
		    SyncResult syncResult) {
        if (!resourcesReady) {
            return;
        }
        ChatServer chatServer = new ChatServer(context);
        for (Identity identity: mService.getIdentities().getIdentities()) {
            chatServer.refreshList(mService, identity);
        }

    }
}
