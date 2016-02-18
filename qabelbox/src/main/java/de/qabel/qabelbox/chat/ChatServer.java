package de.qabel.qabelbox.chat;

import android.util.Log;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.communication.DropServer;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.storage.ChatMessagesDataBase;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;

/**
 * Created by danny on 17.02.16.
 */
public class ChatServer {

    final String TAG = this.getClass().getSimpleName();
    private DropServer dropServer;
    static ChatServer mInstance;
    public List<ChatServerCallback> callbacks = new ArrayList<>();
    long currentId = System.currentTimeMillis();
    private ArrayList<ChatMessagesDataBase.ChatMessageDatabaseItem> messages = new ArrayList<>();

    public static ChatServer getInstance() {

        if (mInstance == null) {
            mInstance = new ChatServer();
        }
        return mInstance;
    }

    private ChatServer() {

        mInstance = this;
        mInstance.dropServer = new DropServer();
    }

    public void addListner(ChatServerCallback callback) {

        callbacks.add(callback);
    }

    public void removeListner(ChatServerCallback callback) {

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
                                return;
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

    protected void parseGetResponse(Response response, ChatStreamParser result) throws IOException, MimeException {

        response.header("content-type");
        MimeStreamParser parser = new MimeStreamParser();
        parser.setContentDecoding(true);
        parser.setContentHandler(result);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Headers headers = response.headers();

        //transmit header to mime4j
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
        for (int i = 0; i < result.parts.size(); i++) {
            byte[] part = result.parts.get(i);
            try {
                JSONObject json = new JSONObject(new String(part));
                messages.add(createOtherMessage(json));
                addMessagesFromDataBase(messages);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.v(TAG, "danny: " + new String(part));
        }
    }

    private void addMessagesFromDataBase(ArrayList<ChatMessagesDataBase.ChatMessageDatabaseItem> messages) {
        //@todo add older (own) and loaded messages
    }

    public ChatMessagesDataBase.ChatMessageDatabaseItem createOtherMessage(JSONObject pJson) {

        ChatMessagesDataBase.ChatMessageDatabaseItem item = new ChatMessagesDataBase.ChatMessageDatabaseItem();
        JSONObject json = new JSONObject();
        try {
            item.time_stamp = pJson.getLong("time_stamp");
            item.sender = pJson.getString("sender");
            item.receiver = pJson.getString("receiver");
            JSONObject payload = pJson.getJSONObject("data");
            json.put("message", payload.getString("message"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        item.drop_payload = json.toString();
        item.isNew = 1;

        return item;
    }

    public ChatMessagesDataBase.ChatMessageDatabaseItem createOwnMessage(String receiverKey,String message) {

        String identityPublicKey = QabelBoxApplication.getInstance().getService().getActiveIdentity().getEcPublicKey().getReadableKeyIdentifier().toString();
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
        item.isNew = 1;

        return item;
    }

    public void sendTextMessage(final long ownId, String dropId, String text, Identity currentIdentity, String receiver) {

        dropServer.push(dropId, text, receiver, currentIdentity, new Callback() {
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
                    sendCallbacksSuccess(ownId);
                } else {
                    sendCallbacksError(ownId);
                }
            }
        });
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

    public ArrayList<ChatMessagesDataBase.ChatMessageDatabaseItem> getAllItemsForKey(String contactPublicKey) {

        ArrayList<ChatMessagesDataBase.ChatMessageDatabaseItem> result = new ArrayList<>();
        for (ChatMessagesDataBase.ChatMessageDatabaseItem message : messages) {
            result.add(message);
        }
        return result;
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
