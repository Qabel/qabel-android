package de.qabel.qabelbox.providers;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.chat.ChatMessagesDataBase;
import de.qabel.qabelbox.services.LocalQabelService;

public class InternalContentProvider extends ContentProvider {
    Context context;
    LocalQabelService mService;
    Map<Identity, ChatMessagesDataBase> dataBases;
    private Set<Identity> knownIdentities;

    public InternalContentProvider() {

    }

    public InternalContentProvider(Context context) {
        this.context = context;
    }

    void bindToService(final Context context) {

        Intent intent = new Intent(context, LocalQabelService.class);
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                LocalQabelService.LocalBinder binder = (LocalQabelService.LocalBinder) service;
                if (binder != null) {
                    mService = binder.getService();
                    initDatabases();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }
        }, Context.BIND_AUTO_CREATE);
    }

    void initDatabases() {
        dataBases = new HashMap<>();
        knownIdentities = getIdentities();
        for (Identity identity: knownIdentities) {
            dataBases.put(identity, new ChatMessagesDataBase(context, identity));
        }
    }

    @Override
    public boolean onCreate() {
        if (context == null) {
            context = getContext();
        }
        bindToService(context);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    Set<Identity> getIdentities() {
        Identities identities = mService.getIdentities();
        return identities.getIdentities();
    }

    Map<Identity, ChatMessagesDataBase> getDataBases() {
        if (!knownIdentities.equals(getIdentities())) {
            initDatabases();
        }
        return dataBases;
    }
}
