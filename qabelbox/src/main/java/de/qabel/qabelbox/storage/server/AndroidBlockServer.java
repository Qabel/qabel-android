package de.qabel.qabelbox.storage.server;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;

import javax.inject.Inject;

import de.qabel.qabelbox.communication.BaseServer;
import de.qabel.qabelbox.communication.UploadRequestBody;
import de.qabel.qabelbox.communication.callbacks.DownloadRequestCallback;
import de.qabel.qabelbox.communication.callbacks.JSONModelCallback;
import de.qabel.qabelbox.communication.callbacks.RequestCallback;
import de.qabel.qabelbox.communication.callbacks.UploadRequestCallback;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.storage.model.BoxQuota;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AndroidBlockServer extends BaseServer implements BlockServer {

    private final static String TAG = AndroidBlockServer.class.getSimpleName();
    public static final String BLOCKS = "blocks/";
    private int currentId = 0;
    private final int suffixId;

    public AndroidBlockServer(AppPreference preference, Context context) {
        super(preference, context);
        //maybe it can be bether to create a unique id. but normally we have only one instance in boxvolume of blockserver so it should no collision occurs
        suffixId = (this.getClass().hashCode() % 0xffff) * 0x10000;
    }

    private void doFileServerAction(String prefix, String path, String method,
                                    RequestBody body, RequestCallback callback,
                                    String ifModified) {
        Request.Builder builder = new Request.Builder()
                .url(urlForFile(prefix, path));

        builder = builder.method(method, body);

        addHeader(getToken(), builder);
        if (ifModified != null) {
            builder.addHeader("If-None-Match", ifModified);
        }
        Request request = builder.build();
        Log.v(TAG, "blockserver request " + request.toString());

        doRequest(request, callback);
    }

    @Override
    public void downloadFile(String prefix, String path, String ifModified, DownloadRequestCallback callback) {
        doFileServerAction(prefix, path, "GET", null, callback, ifModified);
    }

    @Override
    public void uploadFile(String prefix, String name, File file, UploadRequestCallback callback) {
        doFileServerAction(prefix, name, "POST", new UploadRequestBody(file, JSON, callback), callback, null);
    }

    @Override
    public void deleteFile(String prefix, String path, RequestCallback callback) {
        doFileServerAction(prefix, path, "DELETE", null, callback, null);
    }

    @Override
    public void getQuota(JSONModelCallback<BoxQuota> callback) {
        String url = getUrls().getBaseBlock() + BlockServer.API_QUOTA;
        doServerAction(url, null, callback, getToken());
    }

    @Override
    public String urlForFile(String prefix, String path) {
        String apiURL = getUrls().getFiles();
        Uri.Builder uriBuilder = Uri.parse(apiURL).buildUpon()
                .appendPath(prefix);
        if (path.startsWith(BLOCKS)) {
            uriBuilder.appendPath("blocks");
            path = path.substring(BLOCKS.length());
        }
        return uriBuilder
                .appendPath(path)
                .build().toString();
    }

}
