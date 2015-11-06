package de.qabel.qabelbox.adapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class BoxSyncAdapter extends AbstractThreadedSyncAdapter {

    ContentResolver mContentResolver;
    Context context;

    public BoxSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        this.context = context;
        mContentResolver = context.getContentResolver();
        Log.wtf("asd", "asdasd");

    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.wtf("asd", "syyyyyyyyyyyyyyyyyyyyyyyyyyyyync");
    }
}
