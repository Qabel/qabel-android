package de.qabel.qabelbox.adapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;
import de.qabel.desktop.repository.sqlite.SqliteContactRepository;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.persistence.RepositoryFactory;
import de.qabel.qabelbox.services.DropConnector;
import de.qabel.qabelbox.services.HttpDropConnector;

public class QabelSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "QabelSyncAdapter";
    ContentResolver mContentResolver;
    Context context;
    RepositoryFactory factory;

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
        factory = new RepositoryFactory(context);
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
        Set<Identity> identities;
        try {
            identities = getIdentities().getIdentities();
        } catch (SQLException | PersistenceException e) {
            Log.e(TAG, "Sync failed", e);
            return;
        }
        for (Identity identity: identities) {
            Log.i(TAG, "Loading messages for identity "+ identity.getAlias());
            try {
                chatServer.refreshList(getDropConnector(), identity);
            } catch (SQLException | PersistenceException e) {
                Log.e(TAG, "Drop message retrieval failed", e);
                return;
            }
        }
        sendRefreshIntents();
    }

    private void sendRefreshIntents() {
        Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        context.sendBroadcast(intent);
        Intent chatIntent = new Intent(Helper.INTENT_REFRESH_CHAT);
        context.sendBroadcast(chatIntent);
    }

    private Identities getIdentities() throws SQLException, PersistenceException {
        AndroidClientDatabase database = factory.getAndroidClientDatabase();
        return factory.getIdentityRepository(database).findAll();
    }

    public DropConnector getDropConnector() throws SQLException, PersistenceException {
        return new HttpDropConnector(getIdentities(), getContacts());
    }

    private Map<Identity, Contacts> getContacts() throws SQLException, PersistenceException {
        SqliteContactRepository contactRepository = factory.getContactRepository(
                factory.getAndroidClientDatabase());
        Map<Identity, Contacts> contactMap = new HashMap<>();
		for (Identity identity : getIdentities().getIdentities()) {
			contactMap.put(identity, contactRepository.find(identity));
		}
        return contactMap;
    }
}
