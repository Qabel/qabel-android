package de.qabel.qabellauncher.service;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.qabel.QabelContentProviderConstants;
import de.qabel.ackack.MessageInfo;
import de.qabel.ackack.Responsible;
import de.qabel.ackack.event.EventActor;
import de.qabel.ackack.event.EventListener;
import de.qabel.core.EventNameConstants;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.config.ResourceActor;
import de.qabel.qabellauncher.QabelLauncherApplication;

/**
 * QabelResourceProvider provides access to Qabel resources like Contacts and Identities for
 * Qabel client applications.
 *
 * QabelResourceProvider hosts the ResourceActorThread and requires the database to be unlocked.
 * Thus before IQabelServiceInternal.RESOURCES_INITIALIZED is received, the provider will only
 * return null values.
 */
public class QabelContentProvider extends ContentProvider {

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int CONTACTS = 1;
    private static final int IDENTITIES = 2;

    private final Contacts contacts;
    private final Identities identities;
    private ResourceActor resourceActor;
    private ProviderActor providerActor;
    private Thread providerActorThread;
    private boolean resourcesReady;

    static {
        uriMatcher.addURI(QabelContentProviderConstants.CONTENT_AUTHORITY, QabelContentProviderConstants.CONTENT_CONTACTS, CONTACTS);
        uriMatcher.addURI(QabelContentProviderConstants.CONTENT_AUTHORITY, QabelContentProviderConstants.CONTENT_IDENTITIES, IDENTITIES);
    }

    public QabelContentProvider() {
        contacts = new Contacts();
        identities = new Identities();
    }

    /**
     * Loads qabel resources from ResourceActor
     */
    class ProviderActor extends EventActor implements EventListener {
        public ProviderActor() {
            on(EventNameConstants.EVENT_CONTACT_ADDED, this);
            on(EventNameConstants.EVENT_IDENTITY_ADDED, this);

            resourceActor.retrieveContacts(this, new Responsible() {
                @Override
                public void onResponse(Serializable... data) {
                    for (Contact c : (Contact[]) data) {
                        contacts.put(c);
                    }
                }
            });

            resourceActor.retrieveIdentities(this, new Responsible() {
                @Override
                public void onResponse(Serializable... data) {
                    for (Identity identity : (Identity[]) data) {
                        identities.put(identity);
                    }
                }
            });
        }

        @Override
        public void onEvent(String event, MessageInfo info, Object... data) {
            switch (event) {
                case EventNameConstants.EVENT_CONTACT_ADDED:
                    if (data[0] instanceof Contact) {
                        contacts.put((Contact) data[0]);
                    }
                    break;
                case EventNameConstants.EVENT_IDENTITY_ADDED:
                    if (data[0] instanceof Identity) {
                        identities.put((Identity) data[0]);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Starts initialization of QabelResourceProvider resources when global resources are ready
     */
    class ResourceReadyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            resourceActor = QabelLauncherApplication.getResourceActor();

            providerActor = new ProviderActor();
            providerActorThread = new Thread(providerActor, "ProviderActorThread");
            providerActorThread.start();

            resourcesReady = true;
        }
    }

    @Override
    public boolean onCreate() {
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(new ResourceReadyReceiver(),
                new IntentFilter(QabelLauncherApplication.RESOURCES_INITIALIZED));
        return true;
    }

    private Cursor queryContacts() {
        MatrixCursor cursor = new MatrixCursor(QabelContentProviderConstants.CONTACT_COLUMN_NAMES);

        ArrayList<Contact> returnedContactList = new ArrayList<>(contacts.getContacts());
        Collections.sort(returnedContactList, new Comparator<Contact>() {
            @Override
            public int compare(Contact lhs, Contact rhs) {
                return lhs.getAlias().compareTo(rhs.getAlias());
            }
        });

        for (Contact contact : returnedContactList) {
            String[] values = new String[]{contact.getAlias(), contact.getContactOwnerKeyId(),
                    contact.getKeyIdentifier()};
            cursor.addRow(values);
        }
        return cursor;
    }

    private Cursor queryIdentities() {
        MatrixCursor cursor = new MatrixCursor(QabelContentProviderConstants.IDENTITIES_COLUMN_NAMES);

        ArrayList<Identity> returnedIdentityList = new ArrayList<>(identities.getIdentities());
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