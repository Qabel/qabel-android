package de.qabel.qabelbox.communication;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ProfileServer extends BaseServer {

    private final static String TAG = "ProfileServer";

    private void doServerAction(String url, Callback callback, String token) {

        Request.Builder builder = new Request.Builder()
                .url(url);
        addHeader(token, builder);
        Request request = builder.build();
        Log.d(TAG, "request " + request.toString());
        client.newCall(request).enqueue(callback);
    }

    public void getProfile(Context context, Callback callback) {
        doServerAction(urls.getProfile(), callback, new AppPreference(context).getToken());
    }

    public static ServerResponse parseJson(JSONObject json) throws JSONException {

        ServerResponse response = new ServerResponse();
		if (json.has("quota")) {
			response.quota = json.getLong("quota");
		}
		if (json.has("used_storage")) {
			response.usedStorage = json.getLong("used_storage");
		}
		return response;
    }

    public final static class ServerResponse {

        public Long quota;
        public Long usedStorage;
    }
}
