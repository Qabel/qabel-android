package de.qabel.qabelbox.communication;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by danny on 26.01.2016.
 * <p/>
 * Class to simple handle a server response.
 */
public abstract class SimpleCallback implements Callback {

    private String TAG = "callback";//.getClass().getSimpleName();
    protected int retryCount = 0;

    protected enum Reasons {
        IOException, Body, InvalidResponse
    }

    /**
     * call when any server error occurs
     *
     * @param call
     * @param IOException
     */
    protected abstract void onError(Call call, Reasons IOException);

    /**
     * call when response is ok.
     *
     * @param call     hold the server call object
     * @param response hold the response object
     * @param text     contain the body as string
     */
    protected abstract void onSuccess(Call call, Response response, String text);

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
            Log.v(TAG, "response " + response.toString());
            onError(call, Reasons.InvalidResponse);
            return;
        }
        String text;
        try {
            text = response.body().string();
        } catch (IOException e) {
            Log.w(TAG, "server response can't parse", e);
            onError(call, Reasons.Body);
            return;
        }
        Log.d(TAG, "server response valid " + text);
        onSuccess(call, response, text);
    }
}
