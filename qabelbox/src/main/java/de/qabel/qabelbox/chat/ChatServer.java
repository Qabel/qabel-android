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
    public static final String SHARE_NOTIFICATION = "box_share_notification";
    public static final String BOX_MESSAGE = "box_message";
    private ChatMessagesDataBase dataBase;
    private static ChatServer mInstance;
    private final List<ChatServerCallback> callbacks = new ArrayList<>();
    private long currentId = System.currentTimeMillis();
    private final ArrayList<ChatMessageItem> messages = new ArrayList<>();
    private Identity mIdentity;

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

        if (mInstance == null) {
            mInstance = new ChatServer(activeIdentity);
        }
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

        return QabelBoxApplication.getInstance().getService().retrieveDropMessages();
    }

    public void addMessagesFromDataBase(ArrayList<ChatMessageItem> messages) {

        ChatMessageItem[] result = dataBase.getAll();
        if (result != null) {
            for (ChatMessageItem item : result) {

                Log.d(TAG, "add messages from database " + item.drop_payload);
                messages.add(item);
            }
        }
    }

    /**
     * create own chat message to store in db
     *
     * @param receiverKey receiver key
     * @param message     message
     * @return
     */
    public ChatMessageItem createOwnMessage(Identity mIdentity,String receiverKey, String message) {

        String identityPublicKey = mIdentity.getEcPublicKey().getReadableKeyIdentifier();
        ChatMessageItem item = new ChatMessageItem();
        item.time_stamp = System.currentTimeMillis();
        item.sender = identityPublicKey;
        item.receiver = receiverKey;
        JSONObject json = new JSONObject();
        try {
            json.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        item.drop_payload = json.toString();
        item.drop_payload_type = ChatMessageItem.BOX_MESSAGE;
        item.isNew = 1;

        return item;
    }

    private void storeIntoDB(ChatMessageItem item) {

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

    public synchronized long getNextId() {

        return currentId++;
    }

    public DropMessage getTextDropMessage(String message) {

        String payload_type = BOX_MESSAGE;
        JSONObject payloadJson = new JSONObject();
        try {
            payloadJson.put("message", message);
        } catch (JSONException e) {
            Log.e(TAG, "error on create json", e);
        }
        String payload = payloadJson.toString();
        DropMessage dm = new DropMessage(QabelBoxApplication.getInstance().getService().getActiveIdentity(), payload, payload_type);
        return dm;
    }

    public void storeOwnInDb(ChatMessageItem ownMessage) {

        dataBase.put(ownMessage);
    }

    public interface ChatServerCallback {

        //chatlist refreshed
        void onRefreshed();
    }
}
