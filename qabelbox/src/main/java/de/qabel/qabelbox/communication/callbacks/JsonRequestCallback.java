package de.qabel.qabelbox.communication.callbacks;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Response;

/**
 * Use onJSONSuccess to directly receive a JSONObject from the Server response.
 */
public abstract class JsonRequestCallback extends RequestCallback {

    private static final String TAG = JsonRequestCallback.class.getCanonicalName();

    public JsonRequestCallback() {
        super();
    }

    public JsonRequestCallback(int[] acceptedStatusCodes) {
        super(acceptedStatusCodes);
    }

    protected abstract void onJSONSuccess(Response response, JSONObject result);

    @Override
    protected void onSuccess(int statusCode, Response response) {
        String text = null;
        try {
            text = response.body().string();
        } catch (IOException e) {
            Log.w(TAG, "server response can't parse ", e);
            if (text != null) {
                Log.v(TAG, text);
            }
            onError(e, response);
            return;
        }

        try {
            JSONObject json = new JSONObject(text);
            onJSONSuccess(response, json);
        } catch (JSONException e) {
            Log.w(TAG, "server response can't parse json", e);
            onError(e, response);
            return;
        }
    }
}
