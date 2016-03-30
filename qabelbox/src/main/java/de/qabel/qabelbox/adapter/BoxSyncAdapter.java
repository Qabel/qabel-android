package de.qabel.qabelbox.adapter;

import android.accounts.Account;
import android.content.*;
import android.os.Bundle;

public class BoxSyncAdapter extends AbstractThreadedSyncAdapter {
    ContentResolver mContentResolver;
    Context context;

    public BoxSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        this.context = context;
        mContentResolver = context.getContentResolver();

    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
    }
}
