package de.qabel.qabelbox.chat;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.qabelbox.QabelBoxApplication;

/**
 * Created by danny on 17.02.16.
 */
public class ChatServer {

	private static final String TAG = "ChatServer";

	private ChatMessagesDataBase dataBase;
	private final List<ChatServerCallback> callbacks = new ArrayList<>();

	public ChatServer(Identity currentIdentity) {

		dataBase = new ChatMessagesDataBase(QabelBoxApplication.getInstance(), currentIdentity);
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
		long lastRetrieved = dataBase.getLastRetrievedDropMessageTime();
		Log.d(TAG, "last retrieved dropmessage time " + lastRetrieved + " / " + System.currentTimeMillis());
		Collection<DropMessage> result = QabelBoxApplication.getInstance().getService().retrieveDropMessages(QabelBoxApplication.getInstance().getService().getActiveIdentity(),lastRetrieved);

		if (result != null) {
			Log.d(TAG, "new message count: " + result.size());
			//store into db
			for (DropMessage item : result) {
				ChatMessageItem cms = new ChatMessageItem(item);
				cms.isNew = 0;
				dataBase.put(cms);
			}

			//@todo replace this with header from server response.
			//@see https://github.com/Qabel/qabel-android/issues/272
			for (DropMessage item : result) {
				lastRetrieved = Math.max(item.getCreationDate().getTime(), lastRetrieved);
			}
		}
		lastRetrieved = 0;
		dataBase.setLastRetrivedDropMessagesTime(lastRetrieved);
		Log.d(TAG, "new retrieved dropmessage time " + lastRetrieved);

		sendCallbacksRefreshed();
		return result;
	}


	public void addMessagesFromDataBase(ArrayList<ChatMessageItem> messages) {

		ChatMessageItem[] result = dataBase.getAll();
		if (result != null) {
			Collections.addAll(messages, result);
		}
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
			payloadJson.put("msg", message);
		} catch (JSONException e) {
			Log.e(TAG, "error on create json", e);
		}
		String payload = payloadJson.toString();
		return new DropMessage(QabelBoxApplication.getInstance().getService().getActiveIdentity(), payload, payload_type);
	}

	public DropMessage getShareDropMessage(String message, String url, String key) {

		String payload_type = ChatMessageItem.SHARE_NOTIFICATION;
		JSONObject payloadJson = new JSONObject();
		try {
			payloadJson.put("msg", message);
			payloadJson.put("url", url);
			payloadJson.put("key", key);
		} catch (JSONException e) {
			Log.e(TAG, "error on create json", e);
		}
		String payload = payloadJson.toString();
		return new DropMessage(QabelBoxApplication.getInstance().getService().getActiveIdentity(), payload, payload_type);
	}

	public boolean hasNewMessages(Contact c) {
		return dataBase.getNewMessageCount(c) > 0;
	}

	public int setAllMessagesReaded(Contact c) {
		return dataBase.setAllMessagesReaded(c);
	}

	public interface ChatServerCallback {

		//droplist refreshed
		void onRefreshed();
	}
}
