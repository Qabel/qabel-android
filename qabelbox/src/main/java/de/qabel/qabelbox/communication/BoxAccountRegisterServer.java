package de.qabel.qabelbox.communication;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.qabel.qabelbox.communication.callbacks.JsonRequestCallback;
import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by danny on 26.01.2016.
 * <p>
 * class to handle the register server network action
 */
public class BoxAccountRegisterServer extends BaseServer {

    private final static String TAG = "BoxAccountServer";

    private static final String JSON_KEY = "key";
    private static final String JSON_USERNAME = "username";
    private static final String JSON_PASSWORD = "password";
    private static final String JSON_EMAIL = "email";
    private static final String JSON_PASSWORD_OLD = "old_password";
    private static final String JSON_PASSWORD_1 = "password1";
    private static final String JSON_PASSWORD_2 = "password2";
    private static final String JSON_NON_FIELD_ERRORS = "non_field_errors";
    private static final String JSON_SUCCESS = "success";
    private static final String JSON_PASSWORD_NEW_1 = "new_password1";
    private static final String JSON_PASSWORD_NEW_2 = "new_password2";

    /**
     * main function for server action
     *
     * @param url
     * @param json
     * @param callback
     * @param token
     */
    private void doServerAction(String url, JSONObject json, JsonRequestCallback callback, String token) {

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);
        addHeader(token, builder);
        final Request request = builder.build();
        Log.v(TAG, "request: " + request.toString() + " JSON: " + json.toString());
        doRequest(request, callback);
    }

    private void doServerAction(String url, JSONObject json, JsonRequestCallback callback) {
        doServerAction(url, json, callback, null);
    }

    public void register(String username, String password1, String password2, String email, JsonRequestCallback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put(JSON_USERNAME, username);
            json.put(JSON_PASSWORD_1, password1);
            json.put(JSON_PASSWORD_2, password2);
            json.put(JSON_EMAIL, email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(urls.getRegister(), json, callback);
    }

    public void login(String username, String password, JsonRequestCallback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put(JSON_USERNAME, username);
            json.put(JSON_PASSWORD, password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(urls.getLogin(), json, callback);
    }

    public void logout(Context context, JsonRequestCallback callback) {

        JSONObject json = new JSONObject();
        doServerAction(urls.getLogout(), json, callback, new AppPreference(context).getToken());
    }

    public void changePassword(Context context, String old_password, String new_password1, String new_password2, JsonRequestCallback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put(JSON_PASSWORD_NEW_1, new_password1);
            json.put(JSON_PASSWORD_NEW_2, new_password2);
            json.put(JSON_PASSWORD_OLD, old_password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(urls.getPasswordChange(), json, callback, new AppPreference(context).getToken());
    }

    public void resetPassword(String email, JsonRequestCallback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put(JSON_EMAIL, email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(urls.getPasswordReset(), json, callback);
    }

    /**
     * parse all know server response fields, if available
     *
     * @param json
     * @return
     */
    public static ServerResponse parseJson(JSONObject json) {

        ServerResponse response = new ServerResponse();
        response.token = getJsonString(JSON_KEY, json);
        response.username = getJsonString(JSON_USERNAME, json);
        response.password = getJsonString(JSON_PASSWORD, json);
        response.email = getJsonString(JSON_EMAIL, json);
        response.password1 = getJsonString(JSON_PASSWORD_1, json);
        response.password2 = getJsonString(JSON_PASSWORD_2, json);
        response.non_field_errors = getJsonString(JSON_NON_FIELD_ERRORS, json);
        response.old_password = getJsonString(JSON_PASSWORD_OLD, json);
        response.success = getJsonString(JSON_SUCCESS, json);
        response.new_password1 = getJsonString(JSON_PASSWORD_NEW_1, json);
        response.new_password1 = getJsonString(JSON_PASSWORD_NEW_2, json);
        return response;
    }


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
