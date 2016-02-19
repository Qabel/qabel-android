package de.qabel.qabelbox.chat;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.qabelbox.QabelBoxApplication;

/**
 * Created by danny on 17.02.16.
 */
public class ChatServer {

    private static final String TAG = "ChatServer";

    private ChatMessagesDataBase dataBase;
    private static ChatServer mInstance;
    private final List<ChatServerCallback> callbacks = new ArrayList<>();

    public static ChatServer getInstance() {

        if (mInstance == null) {
            Log.e(TAG, "chatServer instance is null. Maybe forgot initialize with identity?");
        }
        return mInstance;
    }

    private ChatServer(Identity currentIdentity) {

        mInstance = this;
        mInstance.dataBase = new ChatMessagesDataBase(QabelBoxApplication.getInstance(), currentIdentity);
    }

    public static ChatServer getInstance(Identity activeIdentity) {
        mInstance = new ChatServer(activeIdentity);
        return mInstance;
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

    public Collection<DropMessage> refreshList() {

        Collection<DropMessage> result = QabelBoxApplication.getInstance().getService().retrieveDropMessages();
        sendCallbacksRefreshed();
        return result;
    }

    public void addMessagesFromDataBase(ArrayList<ChatMessageItem> messages) {

        ChatMessageItem[] result = dataBase.getAll();
        if (result != null) {
            for (ChatMessageItem item : result) {
                messages.add(item);
            }
        }
    }

    /**
     * create own chat message to store in db
     */
    public ChatMessageItem createOwnMessage(Identity mIdentity, String receiverKey, String payload, String payload_type) {

        ChatMessageItem item = new ChatMessageItem();
        item.time_stamp = System.currentTimeMillis();
        item.sender = mIdentity.getEcPublicKey().getReadableKeyIdentifier();
        item.receiver = receiverKey;
        item.drop_payload = payload;
        item.drop_payload_type = payload_type;
        item.isNew = 1;

        return item;
    }

    public void storeIntoDB(ChatMessageItem item) {

        if (item != null) {
            dataBase.put(item);
        }
    }

    /**
     * send all listener that chatmessage list was refrehsed
     */
    private void sendCallbacksRefreshed() {

        for (ChatServerCallback callback : callbacks) {
            callback.onRefreshed();
        }
    }

    public DropMessage getTextDropMessage(String message) {

        String payload_type = ChatMessageItem.BOX_MESSAGE;
        JSONObject payloadJson = new JSONObject();
        try {
            payloadJson.put("message", message);
        } catch (JSONException e) {
            Log.e(TAG, "error on create json", e);
        }
        String payload = payloadJson.toString();
        return new DropMessage(QabelBoxApplication.getInstance().getService().getActiveIdentity(), payload, payload_type);
    }

    public static DropMessage getShareDropMessage(String message, String url, String key) {

        String payload_type = ChatMessageItem.SHARE_NOTIFICATION;
        JSONObject payloadJson = new JSONObject();
        try {
            payloadJson.put("message", message);
            payloadJson.put("url", url);
            payloadJson.put("key", key);
        } catch (JSONException e) {
            Log.e(TAG, "error on create json", e);
        }
        String payload = payloadJson.toString();
        return new DropMessage(QabelBoxApplication.getInstance().getService().getActiveIdentity(), payload, payload_type);
    }

    public interface ChatServerCallback {

        //droplist refreshed
        void onRefreshed();
    }
}
