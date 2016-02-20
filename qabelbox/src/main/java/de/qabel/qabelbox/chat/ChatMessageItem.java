package de.qabel.qabelbox.chat;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by danny on 16.02.16.
 */
public class ChatMessageItem {

    public static final String SHARE_NOTIFICATION = "box_share_notification";
    public static final String BOX_MESSAGE = "box_message";

    //from json
    public int version;
    public long time_stamp;
    public String sender;
    public String receiver;
    public String acknowledge_id;//key
    public String drop_payload_type;//key
    public String drop_payload;
    private String TAG = this.getClass().getSimpleName();

    public short isNew = 1;//1 for new, 0 for old
    public int id;

    //inten values for db storage

    public long getTime() {

        return time_stamp;
    }

    public String getSenderKey() {

        return sender;
    }

    public String getReceiverKey() {

        return receiver;
    }

/*    public Type getModelObject()

    {

        if (drop_payload_type == null) {
            return null;
        }
        if (drop_payload_type.equals(SHARE_NOTIFICATION)) {
            return Type.SHARE_NOTIFICATION;
        } else if (drop_payload_type.equals(BOX_MESSAGE)) {
            return Type.BOX_MESSAGE;
        } else {
            return Type.UNKNOWN;
        }
    }*/

    public MessagePayload getData() {

        if (drop_payload_type != null && drop_payload != null) {
            if (drop_payload_type.equals(BOX_MESSAGE)) {
                TextMessagePayload message = new TextMessagePayload();
                try {
                    message.message = new JSONObject(drop_payload).getString("message");
                } catch (JSONException e) {
                    Log.w(TAG, "no payload data field", e);
                    return null;
                }
                return message;
            } else {
                if (drop_payload_type.equals(SHARE_NOTIFICATION)) {
                    ShareMessagePayload message = new ShareMessagePayload();
                    try {
                        JSONObject payload = new JSONObject(drop_payload);
                        message.message = payload.getString("message");
                        message.url = payload.getString("url");
                        message.key = payload.getString("key");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return message;
                }
            }
        }
        return null;
    }

    /**
     * hold text message
     */
    public static class TextMessagePayload extends MessagePayload {

        String message;

        public String getMessage() {

            return message;
        }
    }

    /**
     * hold share message
     */
    public static class ShareMessagePayload extends MessagePayload {

        String message, url, key;

        public String getMessage() {

            return message;
        }

        public String getURL() {

            return url;
        }

        public String getKey() {

            return key;
        }
    }

    public static class MessagePayload {

    }
}

