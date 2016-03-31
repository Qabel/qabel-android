package de.qabel.qabelbox.services;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.qabel.QabelContentProviderConstants;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.BuildConfig;

/**
 * QabelResourceProvider provides access to Qabel resources like Contacts and Identities for
 * Qabel client applications.
 * <p>
 * QabelResourceProvider hosts the ResourceActorThread and requires the database to be unlocked.
 * Thus before IQabelServiceInternal.RESOURCES_INITIALIZED is received, the provider will only
 * return null values.
 */
public class QabelContentProvider extends ContentProvider {

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int CONTACTS = 1;
    private static final int IDENTITIES = 2;
    private static final String TAG = "QabelContentProvider";
    private static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".services.QabelContentProvider";

    private boolean resourcesReady = false;

    static {
        uriMatcher.addURI(CONTENT_AUTHORITY, QabelContentProviderConstants.CONTENT_CONTACTS, CONTACTS);
        uriMatcher.addURI(CONTENT_AUTHORITY, QabelContentProviderConstants.CONTENT_IDENTITIES, IDENTITIES);
    }

    private LocalQabelService mService;

    public QabelContentProvider() {
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        Intent intent = new Intent(context, LocalQabelService.class);
        if (context == null) {
            Log.e(TAG, "Cannot create service without context");
            return false;
        }
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalQabelService.LocalBinder binder = (LocalQabelService.LocalBinder) service;
                mService = binder.getService();
                resourcesReady = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                resourcesReady = false;
                mService = null;
            }
        }, Context.BIND_AUTO_CREATE);

        return true;
    }

    private Cursor queryContacts() {
        MatrixCursor cursor = new MatrixCursor(QabelContentProviderConstants.CONTACT_COLUMN_NAMES);

        for (Contacts contacts : mService.getAllContacts().values()) {
            for (Contact contact : contacts.getContacts()) {
                String[] values = new String[]{contact.getAlias(),
                        contacts.getIdentity().getKeyIdentifier(),
                        contact.getKeyIdentifier()};
                cursor.addRow(values);
            }
        }
        return cursor;
    }

    private Cursor queryIdentities() {
        MatrixCursor cursor = new MatrixCursor(QabelContentProviderConstants.IDENTITIES_COLUMN_NAMES);

        ArrayList<Identity> returnedIdentityList = new ArrayList<>(
                mService.getIdentities().getIdentities());
        Collections.sort(returnedIdentityList, new Comparator<Identity>() {
            @Override
            public int compare(Identity lhs, Identity rhs) {
                return lhs.getAlias().compareTo(rhs.getAlias());
            }
        });

        for (Identity identity : returnedIdentityList) {
            String[] values = new String[]{identity.getAlias(), identity.getKeyIdentifier()};
            cursor.addRow(values);
        }
        return cursor;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!resourcesReady) {
            return null;
        }
        switch (uriMatcher.match(uri)) {
            case CONTACTS: {
                return queryContacts();
            }
            case IDENTITIES: {
                return queryIdentities();
            }
            default:
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        //TODO: Implement
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        //TODO: Implement
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        //TODO: Implement
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        //TODO: Implement
        return 0;
    }
}
