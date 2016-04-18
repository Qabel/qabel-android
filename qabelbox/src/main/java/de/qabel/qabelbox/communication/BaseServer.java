package de.qabel.qabelbox.communication;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.communication.callbacks.RequestCallback;
import de.qabel.qabelbox.communication.connection.ConnectivityManager;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class BaseServer {

    protected final OkHttpClient client;
    protected final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String TAG = "BaseServer";
    URLs urls;

    private ConnectivityManager connectivityManager;
    private List<RequestAction> requestActionQueue = new LinkedList<>();

    /**
     * create new instance of http client and set timeouts
     */
    public BaseServer() {

        connectivityManager = new ConnectivityManager(QabelBoxApplication.getInstance().getApplicationContext());
        connectivityManager.setListener(new ConnectivityManager.ConnectivityListener() {
            @Override
            public void handleConnectionLost() {
                if (!requestActionQueue.isEmpty()) {
                    for (RequestAction action : requestActionQueue) {
                        if (action.isExecuted() && !action.isCanceled()) {
                            action.getCall().cancel();
                        }
                    }
                }
            }

            @Override
            public void handleConnectionEtablished() {
                if (!requestActionQueue.isEmpty()) {
                    List<RequestAction> failedActions = new LinkedList<RequestAction>();
                    for (RequestAction action : requestActionQueue) {
                        if (action.getExecuted() == action.getAutoRetry()) {
                            failedActions.add(action);
                            continue;
                        }
                        if (!action.isExecuted() || action.isCanceled()) {
                            Call call = client.newCall(action.getRequest());
                            call.enqueue(action.getCallback());
                            action.setCall(call);
                        }
                    }
                    requestActionQueue.removeAll(failedActions);
                }
            }
        });

        urls = new URLs();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(15, TimeUnit.SECONDS); // connect timeout
        builder.readTimeout(15, TimeUnit.SECONDS);    // socket timeout
        builder.writeTimeout(10, TimeUnit.SECONDS);
        client = builder.build();
    }

    protected void doRequest(final Request request, RequestCallback callback) {
        final RequestAction requestAction = new RequestAction(request, callback);
        callback.setSystemHandler(new RequestCallback.SystemHandler() {
            @Override
            public void onRequestError() {
                if (requestAction.isExecuted() && !requestAction.isCanceled()) {
                    requestAction.getCall().cancel();
                }
            }

            @Override
            public void onRequestSuccess() {
                requestActionQueue.remove(requestAction);
            }
        });
        if (connectivityManager.isConnected()) {
            Call call = client.newCall(request);
            call.enqueue(callback);
            requestAction.setCall(call);
        }
        requestActionQueue.add(requestAction);
    }

    /**
     * add header to builder
     *
     * @param token   token or null if server no token needed
     * @param builder builder to fill with header
     */
    protected void addHeader(String token, Request.Builder builder) {

        String locale = Locale.getDefault().getLanguage();
        builder.addHeader("Accept-Language", locale);
        builder.addHeader("Accept", "application/json");
        if (token != null) {
            builder.addHeader("Authorization", "Token " + token);
        }
    }

    /**
     * try to parse json objecct as array, otherwise try to get as string.
     *
     * @param key  json keyword
     * @param json json object
     * @return
     */
    protected static String getJsonString(String key, JSONObject json) {

        if (json.has(key)) {
            try {
                return json.getString(key);
            } catch (JSONException e) {
                Log.w(TAG, "can't convert \"+key+\" to string.", e);
            }
        }
        return null;
    }
}
