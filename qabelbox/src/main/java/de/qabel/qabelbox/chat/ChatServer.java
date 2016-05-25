package de.qabel.qabelbox.chat;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.qabelbox.services.DropConnector;

public class ChatServer {

    private static final String TAG = "ChatServer";
    public static final String TAG_MESSAGE = "msg";
    public static final String TAG_URL = "url";
    public static final String TAG_KEY = "key";

    private final List<ChatServerCallback> callbacks = new ArrayList<>();
    private Context context;

    @Inject
    public ChatServer(Context context) {
        this.context = context;

    }

    private ChatMessagesDataBase getDataBaseForIdentity(Identity identity) {
        return new ChatMessagesDataBase(context, identity);
    }


    public void addListener(ChatServerCallback callback) {

        callbacks.add(callback);
    }

    public void removeListener(ChatServerCallback callback) {

        callbacks.remove(callback);
    }

    /**
     * click on refresh button
     */

    public Collection<ChatMessageItem> refreshList(DropConnector connector, Identity identity) {
        try (ChatMessagesDataBase dataBase = getDataBaseForIdentity(identity)) {
            List<ChatMessageItem> messages = new ArrayList<>();
            long lastRetrieved = dataBase.getLastRetrievedDropMessageTime();
            Log.d(TAG, "last retrieved dropmessage time " + lastRetrieved + " / " + System.currentTimeMillis());
            String identityKey = getIdentityIdentifier(identity);
            Collection<DropMessage> result = connector.retrieveDropMessages(identity, lastRetrieved);

            if (result != null) {
                Log.d(TAG, "new message count: " + result.size());
                //store into db
                for (DropMessage item : result) {
                    ChatMessageItem cms = new ChatMessageItem(item);
                    cms.receiver = identityKey;
                    cms.isNew = 1;
                    if (storeIntoDB(dataBase, cms) == ChatMessagesDataBase.MessageStatus.DUPLICATE) {
                        cms.isNew = 0;
                    } else {
                        messages.add(cms);
                    }
                }

                //@todo replace this with header from server response.
                //@see https://github.com/Qabel/qabel-android/issues/272
                for (DropMessage item : result) {
                    lastRetrieved = Math.max(item.getCreationDate().getTime(), lastRetrieved);
                }
            }
            dataBase.setLastRetrievedDropMessagesTime(lastRetrieved);
            Log.d(TAG, "new retrieved dropmessage time " + lastRetrieved);

            sendCallbacksRefreshed();
            dataBase.close();
            return messages;
        }
    }

    private String getIdentityIdentifier(Identity identity) {
        return identity.getEcPublicKey().getReadableKeyIdentifier();
    }


    public ChatMessagesDataBase.MessageStatus storeIntoDB(ChatMessagesDataBase dataBase, ChatMessageItem item) {
        if (item != null) {
            return dataBase.put(item);
        }
        return ChatMessagesDataBase.MessageStatus.ERROR;
    }

    public ChatMessagesDataBase.MessageStatus storeIntoDB(Identity identity, ChatMessageItem item) {
        if (item != null) {
            try (ChatMessagesDataBase db = getDataBaseForIdentity(identity)) {
                return db.put(item);
            }
        }
        return ChatMessagesDataBase.MessageStatus.ERROR;
    }

    /**
     * send all listener that chatmessage list was refrehsed
     */
    public void sendCallbacksRefreshed() {

        for (ChatServerCallback callback : callbacks) {
            callback.onRefreshed();
        }
    }

    public static DropMessage createTextDropMessage(Identity identity, String message) {

        String payload_type = ChatMessageItem.BOX_MESSAGE;
        String payload = createTextDropMessagePayload(message);
        return new DropMessage(identity, payload, payload_type);
    }

    public static String createTextDropMessagePayload(String message) {
        JSONObject payloadJson = new JSONObject();
        try {
            payloadJson.put(TAG_MESSAGE, message);
        } catch (JSONException e) {
            Log.e(TAG, "error on create json", e);
        }
        return payloadJson.toString();
    }

    public DropMessage createShareDropMessage(Identity identity,
                                              String message, String url, String key) {

        String payload_type = ChatMessageItem.SHARE_NOTIFICATION;
        JSONObject payloadJson = new JSONObject();
        try {
            payloadJson.put(TAG_MESSAGE, message);
            payloadJson.put(TAG_URL, url);
            payloadJson.put(TAG_KEY, key);
        } catch (JSONException e) {
            Log.e(TAG, "error on create json", e);
        }
        String payload = payloadJson.toString();
        return new DropMessage(identity, payload, payload_type);
    }


    public boolean hasNewMessages(Identity identity, Contact c) {
        try (ChatMessagesDataBase db = getDataBaseForIdentity(identity)) {
            return db.getNewMessageCount(c) > 0;
        }
    }

    public int setAllMessagesRead(Identity identity, Contact c) {
        try (ChatMessagesDataBase db = getDataBaseForIdentity(identity)) {
            return db.setAllMessagesRead(c);
        }
    }

    public ChatMessageItem[] getAllMessages(Identity identity, Contact c) {
        try (ChatMessagesDataBase db = getDataBaseForIdentity(identity)) {
            return db.get(c.getEcPublicKey().getReadableKeyIdentifier());
        }

    }

    public ChatMessageItem[] getAllMessages(Identity identity) {
        try (ChatMessagesDataBase db = getDataBaseForIdentity(identity)) {
            return db.getAll();
        }
    }

    public interface ChatServerCallback {
        //droplist refreshed
        void onRefreshed();

    }
}
