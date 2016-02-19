package de.qabel.qabelbox.chat;

import android.util.Log;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.communication.DropServer;
import de.qabel.qabelbox.communication.model.ChatMessageItem;
import de.qabel.qabelbox.helper.FileHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;

/**
 * Created by danny on 17.02.16.
 */
public class ChatServer {

    private static final String TAG = "ChatServer";

    private ChatMessagesDataBase dataBase;
    private DropServer dropServer;
    private static ChatServer mInstance;
    private final List<ChatServerCallback> callbacks = new ArrayList<>();
    private long currentId = System.currentTimeMillis();
    private final ArrayList<ChatMessagesDataBase.ChatMessageDatabaseItem> messages = new ArrayList<>();
    private Identity mIdentity;
    CryptoUtils crypto;

    public static ChatServer getInstance() {

        if (mInstance == null) {
            Log.e(TAG, "chatServer instance is null. Maybe forgot initialize with identity?");
        }
        return mInstance;
    }

    private ChatServer(Identity currentIdentity) {

        mInstance = this;
        mInstance.dropServer = new DropServer();
        mInstance.crypto = new CryptoUtils();
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

    public void refreshList(final long ownId, Identity identity) {

        String[] temp = identity.getDropUrls().iterator().next().toString().split("/");
        dropServer.pull(temp[temp.length - 1], new Callback() {
                    public void onFailure(Call call, IOException e) {

                        Log.w(TAG, "get chat message error: ", e);
                        sendCallbacksError(ownId);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        Log.v(TAG, "get chat message code=" + response.code());
                        if (response.code() == 200) {
                            ChatStreamParser result = new ChatStreamParser();
                            try {
                                parseGetResponse(response, result);
                                sendCallbacksSuccess(ownId);
                                refreshMessageList(result);

                                sendCallbacksRefreshed();
                            } catch (MimeException e) {
                                e.printStackTrace();
                                sendCallbacksError(ownId);
                            }
                        } else {
                            sendCallbacksError(ownId);
                        }
                    }
                }

        );
    }

    /**
     * parse response body from server. split multipart messages into single parts
     *
     * @param response response body
     * @param result   interface that implements part handlung
     * @throws IOException
     * @throws MimeException
     */
    private void parseGetResponse(Response response, ChatStreamParser result) throws IOException, MimeException {

        response.header("content-type");
        MimeStreamParser parser = new MimeStreamParser();
        parser.setContentDecoding(true);
        parser.setContentHandler(result);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Headers headers = response.headers();

        //copy header to mime4j stream
        for (String name : headers.names()) {
            String value = headers.get(name);
            baos.write(name.getBytes());
            baos.write(":".getBytes());
            baos.write(value.getBytes());
            baos.write("\n".getBytes());
        }
        //write new line. if not, we lost the first entry
        baos.write("\n".getBytes());

        InputStream bs = response.body().byteStream();
        baos.write(FileHelper.readInputStreamAsData(bs));

        parser.parse(new ByteArrayInputStream(baos.toByteArray()));
    }

    private void refreshMessageList(ChatStreamParser result) {

        Log.d(TAG, "multipart response size: " + result.parts.size());
        messages.clear();
        for (int i = 0; i < result.parts.size(); i++) {
            byte[] part = result.parts.get(i);
            try {
                JSONObject json = new JSONObject(new String(part));
                messages.add(createOtherMessage(json));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        addMessagesFromDataBase(messages);
    }

    private void addMessagesFromDataBase(ArrayList<ChatMessagesDataBase.ChatMessageDatabaseItem> messages) {

        ChatMessagesDataBase.ChatMessageDatabaseItem[] result = dataBase.getAll();
        if (result != null) {
            for (ChatMessagesDataBase.ChatMessageDatabaseItem item : result) {

                Log.d(TAG, "add messages from database " + item.drop_payload);
                messages.add(item);
            }
        }
    }

    /**
     * create received message for db
     *
     * @param pJson
     * @return
     */
    private ChatMessagesDataBase.ChatMessageDatabaseItem createOtherMessage(JSONObject pJson) {

        ChatMessagesDataBase.ChatMessageDatabaseItem item = ChatMessageItem.parseJson(pJson);
        item.isNew = 1;

        return item;
    }

    /**
     * create own chat message to store in db
     *
     * @param receiverKey receiver key
     * @param message     message
     * @return
     */
    public ChatMessagesDataBase.ChatMessageDatabaseItem createOwnMessage(String receiverKey, String message) {

        String identityPublicKey = mIdentity.getEcPublicKey().getReadableKeyIdentifier();
        ChatMessagesDataBase.ChatMessageDatabaseItem item = new ChatMessagesDataBase.ChatMessageDatabaseItem();
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

    /**
     * send text message
     *
     * @param ownId           own tracking id
     * @param dropId          receiver drop id
     * @param text            message to send
     * @param currentIdentity own identity
     * @param contact         receiver
     */
    public JSONObject sendTextMessage(final long ownId, String dropId, String text, Identity currentIdentity, Contact contact) {

        final JSONObject json = ChatMessageItem.getJsonToSend(dropId, text, currentIdentity, contact.getEcPublicKey().getReadableKeyIdentifier().toString());
        Log.d(TAG, "send body: " + json.toString());

        byte[] data = encrypt(contact, json);

        dropServer.push(dropId, data, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                Log.w(TAG, "push chat message error: ", e);
                sendCallbacksError(ownId);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Log.v(TAG, "push chat message code=" + response.code());
                if (response.code() == 200) {
                    Log.d(TAG, "response " + response.body().toString());
                    response.body().close();
                    messages.add(getDataBaseItemFromJson(json));
                    storeIntoDB(getDataBaseItemFromJson(json));
                    sendCallbacksSuccess(ownId);
                } else {
                    sendCallbacksError(ownId);
                }
            }
        });
        return json;
    }

    protected byte[] encrypt(Contact contact, JSONObject json) {

        ByteArrayInputStream plain = new ByteArrayInputStream(json.toString().getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            crypto.encryptStreamAuthenticatedSymmetric(plain, baos, new KeyParameter(contact.getEcPublicKey().getKey()), null);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "error on encryption", e);
        }
        return baos.toByteArray();
    }

    protected byte[] decrypt(KeyParameter key, byte[] data) {

        try {
            byte[] associatedData = null;

            return crypto.decrypt(key, null, data, associatedData);
        } catch (InvalidCipherTextException e) {
            Log.e(TAG, "error on decryption. Maybe message for other user", e);
        }
        return null;
    }

    private ChatMessagesDataBase.ChatMessageDatabaseItem getDataBaseItemFromJson(JSONObject json) {

        ChatMessagesDataBase.ChatMessageDatabaseItem item = ChatMessagesDataBase.ChatMessageDatabaseItem.parseJson(json);
        item.isNew = 0;
        try {
            if (json.has("id"))
                item.id = json.getInt("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return item;
    }

    private void storeIntoDB(ChatMessagesDataBase.ChatMessageDatabaseItem item) {

        if (item != null) {
            dataBase.put(item);
        }
    }

    /**
     * send all listener error notification
     *
     * @param id request id
     */
    private void sendCallbacksError(long id) {

        for (ChatServerCallback callback : callbacks) {
            callback.onError(id);
        }
    }

    /**
     * send all listener success
     *
     * @param id request id
     */
    private void sendCallbacksSuccess(long id) {

        for (ChatServerCallback callback : callbacks) {
            callback.onSuccess(id);
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

    public ArrayList<ChatMessagesDataBase.ChatMessageDatabaseItem> getAllItems() {

        return messages;
    }

    public interface ChatServerCallback {

        //requested command was successfull
        void onSuccess(long id);

        //request command was not successfull
        void onError(long id);

        //chatlist refreshed
        void onRefreshed();
    }
}
