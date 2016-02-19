package de.qabel.qabelbox.communication;

import android.util.Log;

import org.json.JSONObject;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by danny on 26.01.2016.
 * <p/>
 * class to handle the register server network action
 */
public class DropServer extends BaseServer {

    private final static String TAG = "DropServer";

    /**
     * main function for server action
     *  @param url
     * @param data
     * @param callback
     * @param token
     */
    private void doServerAction(String url, byte[] data, Callback callback, String token) {

        Request.Builder builder = new Request.Builder()
                .url(url);
        if (data == null) {
            builder.get();
        } else {
            RequestBody body = RequestBody.create(JSON, data);
            builder.post(body);
        }

        addHeader(token, builder);

        final Request request = builder.build();
        client.newCall(request).enqueue(callback);
        Log.v(TAG, "send request " + request);
    }

    public void push(String dropid, JSONObject json, Callback callback) {

        AppPreference prefs = new AppPreference(QabelBoxApplication.getInstance().getApplicationContext());
        doServerAction(urls.getDrop(dropid), json.toString().getBytes(), callback, prefs.getToken());
    }
    public void push(String dropid, byte[] data, Callback callback) {

        AppPreference prefs = new AppPreference(QabelBoxApplication.getInstance().getApplicationContext());
        doServerAction(urls.getDrop(dropid), data, callback, prefs.getToken());
    }
    public void pull(String dropid, Callback callback) {

        AppPreference app = new AppPreference(QabelBoxApplication.getInstance().getApplicationContext());
        doServerAction(urls.getDrop(dropid), null, callback, app.getToken());
    }
}
