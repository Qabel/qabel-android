package de.qabel.qabelbox.communication;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

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

    private final String TAG = this.getClass().getSimpleName();
    private final OkHttpClient client = new OkHttpClient();
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private void doServerAction(String url, JSONObject json, Callback callback, String token) {


        Log.v(TAG, "post body json" + json.toString());
        RequestBody body = RequestBody.create(JSON, json.toString());
        Log.v(TAG, "post body with " + body.toString());
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);
        if (token != null) {
            builder.addHeader("Authorization", "Token " + token);
        }
        builder.addHeader("Accept","application/json");
        String locale=Locale.getDefault().toString();
        builder.addHeader("language", locale);
        final Request request = builder.build();
        Log.v(TAG, "do server action to " + body + " " + request.toString()+" "+locale);
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

    public void logout(String token, Callback callback) {

        JSONObject json = new JSONObject();
        doServerAction(URLs.LOGOUT, json, callback);
    }

    public void changePassword(String token, String new_password1, String new_password2, String old_password, Callback callback) {

        JSONObject json = new JSONObject();
        try {
            json.put("new_password1", new_password1);
            json.put("new_password2", new_password2);
            json.put("old_password", old_password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doServerAction(URLs.PASSWORD_CHANGE, json, callback);
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
}
