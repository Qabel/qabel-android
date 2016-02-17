package de.qabel.qabelbox.communication.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by danny on 16.02.16.
 */
public class ChatMessageItem {

    //from json
    public int version;
    public long time_stamp;
    public String sender;
    public String acknowledge_id;//key
    public String drop_payload_type;//key
    public String drop_payload;

    //inten values for db storage

    public long getTime() {

        return time_stamp;
    }

    public String getSenderKey() {

        return sender;
    }

    public String getReceiverKey() {

        return acknowledge_id;
    }

    public Type getModelObject()

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
    }

    public static final String SHARE_NOTIFICATION = "box_share_notification";
    public static final String BOX_MESSAGE = "box_message";

    public MessagePayload getData() {

        MessagePayload message = new MessagePayload();
        try {
            message.message = new JSONObject(drop_payload).getString("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return message;
    }

    public static class MessagePayload

    {

        String message, url, key;

        public String getMessage() {

            return message;
        }
    }

    public enum Type

    {
        SHARE_NOTIFICATION, BOX_MESSAGE, payload, UNKNOWN
    }
}

