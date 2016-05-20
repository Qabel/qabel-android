package de.qabel.qabelbox.adapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;
import de.qabel.qabelbox.chat.ChatMessageInfo;
import de.qabel.qabelbox.chat.ChatMessageItem;
import de.qabel.qabelbox.chat.ChatNotificationManager;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.chat.SyncAdapterChatNotificationManager;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.persistence.RepositoryFactory;
import de.qabel.qabelbox.services.DropConnector;
import de.qabel.qabelbox.services.HttpDropConnector;

public class QabelSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "QabelSyncAdapter";
    ContentResolver mContentResolver;
    Context context;
    RepositoryFactory factory;
    ChatNotificationManager notificationManager;

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
        notificationManager = new SyncAdapterChatNotificationManager(context);
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
        List<ChatMessageItem> retrievedMessages = new ArrayList<>();
        for (Identity identity: identities) {
            Log.i(TAG, "Loading messages for identity "+ identity.getAlias());
            try {
                retrievedMessages.addAll(
                        chatServer.refreshList(getDropConnector(), identity));
            } catch (SQLException | PersistenceException e) {
                Log.e(TAG, "Drop message retrieval failed", e);
                return;
            }
        }
        notifyForNewMessages(retrievedMessages);
    }

    void notifyForNewMessages(List<ChatMessageItem> retrievedMessages) {
        if (retrievedMessages.size() > 0) {
            Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
            context.sendBroadcast(intent);
            Intent chatIntent = new Intent(Helper.INTENT_REFRESH_CHAT);
            context.sendBroadcast(chatIntent);
        }
        updateNotificationManager(retrievedMessages);
    }

    private void updateNotificationManager(List<ChatMessageItem> retrievedMessages) {
        List<ChatMessageInfo> messages = toChatMessageInfo(retrievedMessages);
        notificationManager.updateNotifications(messages);
    }

    private List<ChatMessageInfo> toChatMessageInfo(List<ChatMessageItem> retrievedMessages) {
        ContactRepository contactRepository = getContactRepository();
        IdentityRepository identityRepository = getIdentityRepository();
        List<ChatMessageInfo> messages = new ArrayList<>();
        for (ChatMessageItem msg: retrievedMessages) {
            try {
                Identity identity = identityRepository.find(msg.getReceiverKey());
                Contact contact = contactRepository.findByKeyId(identity, msg.getSenderKey());
                ChatMessageInfo messageInfo = new ChatMessageInfo(
                        contact.getAlias(),
                        identity.getKeyIdentifier(),
                        msg.getData().getMessage(),
                        new Date(msg.getTime()),
                        ChatMessageInfo.MessageType.MESSAGE);
                messages.add(messageInfo);
            } catch (EntityNotFoundExcepion | PersistenceException entityNotFoundExcepion) {
                Log.w(TAG, "Could not find contact " + msg.getSenderKey()
                        +" for identity " + msg.getReceiverKey());
            }
        }
        return messages;
    }

    private IdentityRepository getIdentityRepository() {
        return factory.getIdentityRepository(factory.getAndroidClientDatabase());
    }

    private Identities getIdentities() throws SQLException, PersistenceException {
        AndroidClientDatabase database = factory.getAndroidClientDatabase();
        return factory.getIdentityRepository(database).findAll();
    }

    public DropConnector getDropConnector() throws SQLException, PersistenceException {
        return new HttpDropConnector(getIdentities(), getContacts());
    }

    private Map<Identity, Contacts> getContacts() throws SQLException, PersistenceException {
        ContactRepository contactRepository = getContactRepository();
        Map<Identity, Contacts> contactMap = new HashMap<>();
		for (Identity identity : getIdentities().getIdentities()) {
			contactMap.put(identity, contactRepository.find(identity));
		}
        return contactMap;
    }

    @NonNull
    private ContactRepository getContactRepository() {
        return factory.getContactRepository(factory.getAndroidClientDatabase());
    }
}
