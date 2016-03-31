package de.qabel.qabelbox.communication.callbacks;

import android.util.Log;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by danny on 26.01.2016.
 * <p/>
 * Class to simple handle a server response.
 */
public abstract class SimpleJsonCallback implements Callback {

    private final String TAG = "callback";
    protected int retryCount = 0;

    protected enum Reasons {
        IOException, Body, JSON, InvalidResponse
    }

    /**
     * call when any server error occurs
     */
    protected abstract void onError(Call call, Reasons IOException);

    /**
     * call when response is ok.
     *
     * @param call     hold the server call object
     * @param response hold the response object
     * @param json     parsed json object
     */
    protected abstract void onSuccess(Call call, Response response, JSONObject json);

    @Override
    public void onFailure(Call call, IOException e) {

        Log.w(TAG, "error on network action", e);
        onError(call, Reasons.IOException);
    }

    @Override
    public void onResponse(Call call, Response response) {

        int code = response.code();
        Log.v(TAG, "callback onResponse " + response + " " + code);

        if (code != 400 && code < 200 && code >= 300)

        {
            Log.w(TAG, "Unexpected code " + response);
            onError(call, Reasons.InvalidResponse);
            return;
        }
        String text = null;
        try {
            text = response.body().string();
        } catch (IOException e) {
            Log.w(TAG, "server response can't parse ", e);
            if (text != null) {
                Log.v(TAG, text);
            }
            onError(call, Reasons.Body);
            return;
        }

        JSONObject json = null;
        try {
            json = new JSONObject(text);
        } catch (JSONException e) {
            Log.w(TAG, "server response can't parse json", e);
            onError(call, Reasons.JSON);
            return;
        }
        if (json == null) {
            Log.w(TAG, "server response json is empty");
            onError(call, Reasons.JSON);
            return;
        }
        onSuccess(call, response, json);
    }
}
