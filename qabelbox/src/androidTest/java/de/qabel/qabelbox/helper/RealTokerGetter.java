package de.qabel.qabelbox.helper;

import android.content.Context;
import android.util.Log;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.communication.callbacks.SimpleJsonCallback;
import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Call;
import okhttp3.Response;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertNotNull;

/**
 * Created by danny on 01.03.16.
 */
public class RealTokerGetter {
    private String TAG = "RealTokenGetter";
    String token = null;

    public String getToken(final Context context) {
        BoxAccountRegisterServer server = new BoxAccountRegisterServer();
        String un = UUID.randomUUID().toString().substring(0, 23).replace("-", "a");
        String pw1 = "Password12";
        final String email = un + "@qabel.de";
        final CountDownLatch latch = new CountDownLatch(1);
        Log.d(TAG, "get new real token");
        server.register(un, pw1, pw1, email, new SimpleJsonCallback() {
                    protected void onError(final Call call, Reasons reasons) {

                        Log.e(TAG, "cant create new token");
                        latch.countDown();
                    }

                    protected void onSuccess(Call call, Response response, JSONObject json) {
                        BoxAccountRegisterServer.ServerResponse result = BoxAccountRegisterServer.parseJson(json);
                        Log.d(TAG, "token success message " + response.code() + " " + json.toString());
                        if (result.token != null && result.token.length() > 5) {
                            new AppPreference(context).setToken(result.token);
                            token = result.token;
                        }
                        latch.countDown();
                    }
                }
        );
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "new real token from server: " + token);
        assertNotNull(token);
        return token;


    }
}
