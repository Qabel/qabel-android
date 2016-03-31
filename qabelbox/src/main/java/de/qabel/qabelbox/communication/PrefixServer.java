package de.qabel.qabelbox.communication;

import android.content.Context;
import android.util.Log;
import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;

/**
 * Created by danny on 11.02.2016.
 * <p/>
 * class to handle prefix server network action
 */
public class PrefixServer extends BaseServer {

    private final static String TAG = "PrefixServer";

    /**
     * main function for server action
     */
    private void doServerAction(String url, Callback callback, String token) {

        Request.Builder builder = new Request.Builder()
            .post(RequestBody.create(JSON, "{}"))
            .url(url);
        addHeader(token, builder);
        Request request = builder.build();
        Log.d(TAG, "request " + request.toString());
        client.newCall(request).enqueue(callback);
    }

    public void getPrefix(Context context, Callback callback) {

        doServerAction(urls.getPrefix(), callback, new AppPreference(context).getToken());
    }

    /**
     * parse all know server response fields, if available
     */
    public static ServerResponse parseJson(JSONObject json) {

        ServerResponse response = new ServerResponse();
        response.prefix = getJsonString("prefix", json);
        response.detail = getJsonString("detail", json);

        return response;
    }

    public final static class ServerResponse {

        public String prefix;
        public String detail;
    }
}
