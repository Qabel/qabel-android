package de.qabel.qabelbox.communication;

import android.content.Context;
import android.util.Log;
import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import org.json.JSONObject;

public class PrefixServer extends BaseServer {
    private static final String TAG = "PrefixServer";

    /**
     * main function for server action
     */
    private void doServerAction(String url, Callback callback, String token) {
        Builder builder = new Builder()
                .post(RequestBody.create(JSON, "{}"))
                .url(url);
        addHeader(token, builder);
        Request request = builder.build();
        Log.d(TAG, "request " + request);
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

    public static final class ServerResponse {
        public String prefix;
        public String detail;
    }
}
