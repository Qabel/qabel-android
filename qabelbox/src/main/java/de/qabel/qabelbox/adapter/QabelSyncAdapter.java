package de.qabel.qabelbox.adapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import javax.inject.Inject;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;
import de.qabel.qabelbox.QabelBoxApplication;
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
    @Inject Context context;
    @Inject IdentityRepository identityRepository;
    @Inject ContactRepository contactRepository;
    @Inject ChatNotificationManager notificationManager;
    @Inject ChatServer chatServer;
    DropConnector dropConnector;
    private List<ChatMessageInfo> currentMessages = new ArrayList<>();
    private BroadcastReceiver receiver;

    public QabelSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        init(context);
    }

    @Inject
    public void setDropConnector(DropConnector dropConnector) {
        this.dropConnector = dropConnector;
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
        QabelBoxApplication.getApplicationComponent(context).inject(this);
        registerNotificationReceiver();
    }

    private void registerNotificationReceiver() {
        IntentFilter filter = new IntentFilter(Helper.INTENT_REFRESH_CONTACTLIST);
        filter.setPriority(0);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notificationManager.updateNotifications(currentMessages);
            }
        };
        context.registerReceiver(receiver, filter);
    }



    @Override
    public void onPerformSync(
            Account account,
		    Bundle extras,
		    String authority,
		    ContentProviderClient provider,
		    SyncResult syncResult) {
        Log.w(TAG, "Starting drop message sync");
        Set<Identity> identities;
        try {
            identities = identityRepository.findAll().getIdentities();
        } catch (PersistenceException e) {
            Log.e(TAG, "Sync failed", e);
            return;
        }
        List<ChatMessageItem> retrievedMessages = new ArrayList<>();
        for (Identity identity: identities) {
            Log.i(TAG, "Loading messages for identity "+ identity.getAlias());
            retrievedMessages.addAll(
                    chatServer.refreshList(dropConnector, identity));
        }
        notifyForNewMessages(retrievedMessages);
    }

    void notifyForNewMessages(List<ChatMessageItem> retrievedMessages) {
        if (retrievedMessages.size() == 0) {
            return;
        }
        updateNotificationManager(retrievedMessages);
        Intent notificationIntent = new Intent(Helper.INTENT_SHOW_NOTIFICATION);
        context.sendOrderedBroadcast(notificationIntent, null);
        Intent refresh = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        context.sendBroadcast(refresh);
    }

    private void updateNotificationManager(List<ChatMessageItem> retrievedMessages) {
        currentMessages = toChatMessageInfo(retrievedMessages);
    }

    private List<ChatMessageInfo> toChatMessageInfo(List<ChatMessageItem> retrievedMessages) {
        List<ChatMessageInfo> messages = new ArrayList<>();
        for (ChatMessageItem msg: retrievedMessages) {
            try {
                Identity identity = identityRepository.find(msg.getReceiverKey());
                Contact contact = contactRepository.findByKeyId(identity, msg.getSenderKey());
                ChatMessageInfo messageInfo = new ChatMessageInfo(
                        contact,
                        identity,
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

}
