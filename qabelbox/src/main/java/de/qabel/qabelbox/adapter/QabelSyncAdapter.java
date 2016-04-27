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

import java.util.ArrayList;
import java.util.List;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.services.DropConnector;
import de.qabel.qabelbox.services.LocalQabelService;

public class QabelSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "QabelSyncAdapter";
    ContentResolver mContentResolver;
    Context context;

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
    }

    @Override
    public void onPerformSync(
            Account account,
		    Bundle extras,
		    String authority,
		    ContentProviderClient provider,
		    SyncResult syncResult) {
        Log.w(TAG, "Starting drop message sync");
        ChatServer chatServer = new ChatServer(context);
        for (Identity identity: getIdentities()) {
            Log.i(TAG, "Loading messages for identity "+ identity.getAlias());
            chatServer.refreshList(getDropConnector(), identity);
        }
        Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        context.sendBroadcast(intent);
        Intent chatIntent = new Intent(Helper.INTENT_REFRESH_CHAT);
        context.sendBroadcast(chatIntent);
    }

    private List<Identity> getIdentities() {
        return new ArrayList<>();
    }

    public DropConnector getDropConnector() {
        return null;
    }
}
