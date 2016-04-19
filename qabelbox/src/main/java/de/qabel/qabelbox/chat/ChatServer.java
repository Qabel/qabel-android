package de.qabel.qabelbox.chat;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.services.DropConnector;
import de.qabel.qabelbox.services.LocalQabelService;

public class ChatServer {

    private static final String TAG = "ChatServer";
    public static final String TAG_MESSAGE = "msg";
    public static final String TAG_URL = "url";
    public static final String TAG_KEY = "key";

    private final List<ChatServerCallback> callbacks = new ArrayList<>();
    private Context context;

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

    public Collection<DropMessage> refreshList(DropConnector connector, Identity identity) {
        ChatMessagesDataBase dataBase = getDataBaseForIdentity(identity);
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
                storeIntoDB(dataBase, cms);
            }

            //@todo replace this with header from server response.
            //@see https://github.com/Qabel/qabel-android/issues/272
            for (DropMessage item : result) {
                lastRetrieved = Math.max(item.getCreationDate().getTime(), lastRetrieved);
            }
        }
        lastRetrieved = 0;
        dataBase.setLastRetrievedDropMessagesTime(lastRetrieved);
        Log.d(TAG, "new retrieved dropmessage time " + lastRetrieved);

        sendCallbacksRefreshed();
        return result;
    }

    private String getIdentityIdentifier(Identity identity) {
        return identity.getEcPublicKey().getReadableKeyIdentifier();
    }


    public void storeIntoDB(ChatMessagesDataBase dataBase, ChatMessageItem item) {
        if (item != null) {
            dataBase.put(item);
        }
    }

    public void storeIntoDB(Identity identity, ChatMessageItem item) {
        if (item != null) {
            getDataBaseForIdentity(identity).put(item);
        }
    }

    /**
     * send all listener that chatmessage list was refrehsed
     */
    public void sendCallbacksRefreshed() {

        for (ChatServerCallback callback : callbacks) {
            callback.onRefreshed();
        }
    }

    public DropMessage createTextDropMessage(Identity identity, String message) {

        String payload_type = ChatMessageItem.BOX_MESSAGE;
        String payload = createTextDropMessagePayload(message);
        return new DropMessage(identity, payload, payload_type);
    }

    public String createTextDropMessagePayload(String message) {
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
        return getDataBaseForIdentity(identity).getNewMessageCount(c) > 0;
    }

    public int setAllMessagesRead(Identity identity, Contact c) {
        return getDataBaseForIdentity(identity).setAllMessagesRead(c);
    }

    public ChatMessageItem[] getAllMessages(Identity identity, Contact c) {
        return getDataBaseForIdentity(identity).get(c.getEcPublicKey().getReadableKeyIdentifier());
    }

    public ChatMessageItem[] getAllMessages(Identity identity) {
        return getDataBaseForIdentity(identity).getAll();
    }

    public interface ChatServerCallback {
        //droplist refreshed
        void onRefreshed();
    }
}
