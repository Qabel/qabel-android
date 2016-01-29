package de.qabel.qabelbox.communication;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by danny on 26.01.2016.
 * <p/>
 * class to handle the register server network action
 */
public class BoxAccountRegisterServer {

    private final static String TAG = "BoxAccountServer";
    private final OkHttpClient client;
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * create new instance of http client and set timeouts
     */
    public BoxAccountRegisterServer() {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(15, TimeUnit.SECONDS); // connect timeout
        builder.readTimeout(15, TimeUnit.SECONDS);    // socket timeout
        builder.writeTimeout(10, TimeUnit.SECONDS);
        client = builder.build();
    }

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
        if (token != null) {
            builder.addHeader("Authorization", "Token " + token);
        }
        builder.addHeader("Accept", "application/json");
        String locale = Locale.getDefault().toString();
        builder.addHeader("language", locale);
        final Request request = builder.build();
        client.newCall(request).enqueue(callback);
    }

    private void doServerAction(String url, JSONObject json, Callback callback) {

        doServerAction(url, json, callback, null);
    }

    public void register(String username, String password1, String password2, String email, Callback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put("username", username);
            json.put("password1", password1);
            json.put("password2", password2);
            json.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(URLs.REGISTER, json, callback);
    }

    public void login(String username, String password, Callback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put("username", username);
            json.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(URLs.LOGIN, json, callback);
    }

    public void logout(Context context, Callback callback) {

        JSONObject json = new JSONObject();
        doServerAction(URLs.LOGOUT, json, callback, new AppPreference(context).getToken());
    }

    public void changePassword(Context context, String old_password, String new_password1, String new_password2, Callback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put("new_password1", new_password1);
            json.put("new_password2", new_password2);
            json.put("old_password", old_password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(URLs.PASSWORD_CHANGE, json, callback, new AppPreference(context).getToken());
    }

    public void resetPassword(String email, Callback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(URLs.PASSWORD_RESET, json, callback);
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

    /**
     * try to parse json objecct as array, otherwise try to get as string.
     *
     * @param key  json keyword
     * @param json json object
     * @return
     */
    private static String getJsonString(String key, JSONObject json) {

        if (json.has(key)) {
            try {
                JSONArray array = json.getJSONArray(key);
                String ret = "";
                for (int i = 0; i < array.length(); i++) {
                    ret += array.getString(i);
                }
                return ret;
            } catch (JSONException e) {
                Log.d(TAG, "can't convert " + key + " to array. try string");
            }
            try {
                return json.getString(key);
            } catch (JSONException e) {
                Log.w(TAG, "can't convert \"+key+\" to string.", e);
            }
        }
        return null;
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
