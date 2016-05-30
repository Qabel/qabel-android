package de.qabel.qabelbox.communication;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;

import de.qabel.qabelbox.communication.callbacks.DownloadRequestCallback;
import de.qabel.qabelbox.communication.callbacks.RequestCallback;
import de.qabel.qabelbox.communication.callbacks.UploadRequestCallback;
import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Request;
import okhttp3.RequestBody;

public class BlockServer extends BaseServer {

    private final static String TAG = BlockServer.class.getSimpleName();
    public static final String BLOCKS = "blocks/";
    private int currentId = 0;
    private final int suffixId;

    public BlockServer(Context context) {
        super(context);
        //maybe it can be bether to create a unique id. but normally we have only one instance in boxvolume of blockserver so it should no collision occurs
        suffixId = (this.getClass().hashCode() % 0xffff) * 0x10000;
    }

    private void doServerAction(Context context, String prefix, String path, String method, RequestBody body, RequestCallback callback) {
        String apiURL = urls.getFiles();
        Uri.Builder uriBuilder = Uri.parse(apiURL).buildUpon()
                .appendPath(prefix);
        if (path.startsWith(BLOCKS)) {
            uriBuilder.appendPath("blocks");
            path = path.substring(BLOCKS.length());
        }
        String url = uriBuilder
                .appendPath(path)
                .build().toString();
        Request.Builder builder = new Request.Builder()
                .url(url);

        builder = builder.method(method, body);

        addHeader(new AppPreference(context).getToken(), builder);
        Request request = builder.build();
        Log.v(TAG, "blockserver request " + request.toString());

        doRequest(request, callback);
    }

    public void downloadFile(Context context, String prefix, String path, DownloadRequestCallback callback) {
        doServerAction(context, prefix, path, "GET", null, callback);
    }

    public void uploadFile(Context context, String prefix, String name, File file, UploadRequestCallback callback) {
        doServerAction(context, prefix, name, "POST", new UploadRequestBody(file, JSON, callback), callback);
    }

    public void deleteFile(Context context, String prefix, String path, RequestCallback callback) {
        doServerAction(context, prefix, path, "DELETE", null, callback);
    }

    public synchronized int getNextId() {
        return (suffixId + (currentId++) + (int) (System.currentTimeMillis()) % 1000000);
    }
}
