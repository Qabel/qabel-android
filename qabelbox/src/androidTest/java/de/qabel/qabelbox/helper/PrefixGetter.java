package de.qabel.qabelbox.helper;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;

import de.qabel.qabelbox.communication.PrefixServer;
import de.qabel.qabelbox.communication.callbacks.JsonRequestCallback;
import okhttp3.Response;

public class PrefixGetter {

    private String prefix = null;
    private final String TAG = "PrefixGetter";

    public String getPrefix(Context context) {
        final CountDownLatch latch = new CountDownLatch(1);

        new PrefixServer(context).getPrefix(context, new JsonRequestCallback() {

            @Override
            protected void onError(Exception e, @Nullable Response response) {
                Log.w(TAG, "Error communicating with server", e);
                latch.countDown();
            }

            @Override
            protected void onJSONSuccess(Response response, JSONObject json) {
                PrefixServer.ServerResponse result = PrefixServer.parseJson(json);
                Log.d(TAG, "prefix: " + result.prefix);
                prefix = result.prefix;
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return prefix;
    }
}
