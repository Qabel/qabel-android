package de.qabel.qabelbox.communication;

import android.util.Log;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BaseServer {

    protected final OkHttpClient client;
    protected final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String TAG = "BaseServer";
    URLs urls;

    /**
     * create new instance of http client and set timeouts
     */
    public BaseServer() {

        urls = new URLs();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(15, TimeUnit.SECONDS); // connect timeout
        builder.readTimeout(15, TimeUnit.SECONDS);    // socket timeout
        builder.writeTimeout(10, TimeUnit.SECONDS);
        client = builder.build();
    }

    /**
     * add header to builder
     *
     * @param token   token or null if server no token needed
     * @param builder builder to fill with header
     */
    protected void addHeader(String token, Builder builder) {

        String locale = Locale.getDefault().getLanguage();
        builder.addHeader("Accept-Language", locale);
        builder.addHeader("Accept", "application/json");
        if (token != null) {
            builder.addHeader("Authorization", "Token " + token);
            Log.d(TAG, "token " + token);
        }
    }

    /**
     * try to parse json objecct as array, otherwise try to get as string.
     *
     * @param key  json keyword
     * @param json json object
     */
    protected static String getJsonString(String key, JSONObject json) {

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
}
