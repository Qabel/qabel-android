package de.qabel.qabelbox.communication;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import de.qabel.qabelbox.communication.callbacks.JsonRequestCallback;
import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by danny on 11.02.2016.
 * <p/>
 * class to handle prefix server network action
 */
public class PrefixServer extends BaseServer {

    private final static String TAG = "PrefixServer";

    public PrefixServer(Context context) {
        super(context);
    }

    public void getPrefix(Context context, JsonRequestCallback callback) {
        doServerAction(getUrls().getPrefix(), null, callback, getToken());
    }

    /**
     * parse all know server response fields, if available
     *
     * @param json
     * @return
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
