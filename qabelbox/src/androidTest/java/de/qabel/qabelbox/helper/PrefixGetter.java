package de.qabel.qabelbox.helper;

import android.content.Context;
import android.util.Log;
import de.qabel.qabelbox.communication.PrefixServer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by danny on 15.02.2016.
 */
public class PrefixGetter {

    private String prefix = null;
    private final String TAG = "PrefixGetter";

    public String getPrefix(Context context) {
        final CountDownLatch latch = new CountDownLatch(1);

        new PrefixServer().getPrefix(context, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(TAG, "Error communicating with server: " + call, e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String text = response.body().string();
                try {
                    PrefixServer.ServerResponse result = PrefixServer.parseJson(new JSONObject(text));
                    Log.d(TAG, "prefix: " + result.prefix);
                    prefix = result.prefix;

                } catch (JSONException e) {
                    System.out.println(text);
                    if (text != null && text.startsWith("\"") && text.charAt(text.length() - 1) == '"') {
                        prefix = text.substring(1, text.length() - 1);
                        Log.w(TAG, "prefix temp until server fix: " + prefix + " " + text);

                    }
                    Log.w(TAG, "error on parse service response", e);
                }

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
