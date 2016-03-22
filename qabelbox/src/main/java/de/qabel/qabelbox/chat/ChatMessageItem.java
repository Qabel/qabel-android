package de.qabel.qabelbox.chat;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;

/**
 * class to store chatmessage in database
 * <p/>
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
	public String acknowledge_id;
	public String drop_payload_type;
	public String drop_payload;
	private final String TAG = this.getClass().getSimpleName();

	//inten values for db storage
	public short isNew = 1;//1 for new, 0 for old
	public int id = 0;

	public ChatMessageItem(int id, short isNew, long time_stamp, String sender, String receiver, String acknowledge_id, String drop_payload_type, String drop_payload)
	{
		this.id = id;
		this.isNew = isNew;
		this.time_stamp = time_stamp;
		this.sender = sender;
		this.receiver = receiver;
		this.acknowledge_id = acknowledge_id;
		this.drop_payload_type = drop_payload_type;
		this.drop_payload = drop_payload;
	}


	public ChatMessageItem(DropMessage dm) {
		sender = dm.getSenderKeyId();
		acknowledge_id = dm.getAcknowledgeID();
		version = dm.getVersion();
		receiver = null;
		drop_payload = dm.getDropPayload();
		drop_payload_type = dm.getDropPayloadType();
		time_stamp = dm.getCreationDate().getTime();


	}

	public ChatMessageItem(Identity mIdentity, String receiverKey, String payload, String payload_type) {
		time_stamp = System.currentTimeMillis();
		sender = mIdentity.getEcPublicKey().getReadableKeyIdentifier();
		receiver = receiverKey;
		drop_payload = payload;
		drop_payload_type = payload_type;
		isNew = 1;
	}

	public long getTime() {

		return time_stamp;
	}

	public String getSenderKey() {

		return sender;
	}

	public String getReceiverKey() {

		return receiver;
	}

	public MessagePayload getData() {

		if (drop_payload_type != null && drop_payload != null) {
			if (drop_payload_type.equals(BOX_MESSAGE)) {
				TextMessagePayload message = new TextMessagePayload();
				try {
					message.message = new JSONObject(drop_payload).getString(ChatServer.TAG_MESSAGE);
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
						message.message = payload.getString(ChatServer.TAG_MESSAGE);
						message.url = payload.getString(ChatServer.TAG_URL);
						message.key = payload.getString(ChatServer.TAG_KEY);
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

