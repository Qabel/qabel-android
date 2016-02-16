package de.qabel.qabelbox.communication;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

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

    private final static String TAG = "BoxAccountServer";

    /**
     * main function for server action
     *
     * @param url
     * @param json
     * @param callback
     * @param token
     */
    private void doServerAction(String url, JSONObject json, Callback callback, String token) {

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);
        addHeader(token, builder);
        final Request request = builder.build();
        client.newCall(request).enqueue(callback);
    }

    private void doServerAction(String url, JSONObject json, Callback callback) {

        doServerAction(url, json, callback, null);
    }

    public void push(String username, String password1, String password2, String email, Callback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put("username", username);
            json.put("password1", password1);
            json.put("password2", password2);
            json.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(urls.getSendDrop(), json, callback);
    }

    public void pull(String username, String password, Callback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put("username", username);
            json.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(urls.getReceiveDrop(), json, callback);
    }


    /**
     * parse all know server response fields, if available
     *
     * @param json
     * @return
     */
    public static ServerResponse parseJson(JSONObject json) {

        ServerResponse response = new ServerResponse();
        response.token = getJsonString("key", json);
        response.username = getJsonString("username", json);
        response.password = getJsonString("password", json);
        response.email = getJsonString("email", json);
        response.password1 = getJsonString("password1", json);
        response.password2 = getJsonString("password2", json);
        response.non_field_errors = getJsonString("non_field_errors", json);
        response.old_password = getJsonString("old_password", json);
        response.success = getJsonString("success", json);
        response.new_password1 = getJsonString("new_password1", json);
        response.new_password1 = getJsonString("new_password2", json);
        return response;
    }

    //box_share_notification` (was ja durchaus sinn ergibt ^^) und `box_message`

    /**
     * hold all possibility server response fields
     */
    public final static class ServerResponse {

        public String token;
        public String username;
        public String password1;
        public String password2;
        public String email;
        public String non_field_errors;
        public String old_password;
        public String new_password1;
        public String new_password2;
        public String success;
        public String password;
    }
}
