package de.qabel.qabelbox.communication;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;

import java.io.File;

public class BlockServer extends BaseServer {
    private static final String TAG = "PrefixServer";
    public static final String BLOCKS = "blocks/";
    private int currentId;
    private final int suffixId;

    public BlockServer() {
        //maybe it can be bether to create a unique id. but normally we have only one instance in boxvolume of blockserver so it should no collision occurs
        suffixId = getClass().hashCode() % 0xffff * 0x10000;
    }

    private void doServerAction(Context context, String prefix, String path, String method, RequestBody body, Callback callback) {
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
        Builder builder = new Builder()
                .url(url);

        builder = builder.method(method, body);

        addHeader(new AppPreference(context).getToken(), builder);
        Request request = builder.build();
        Log.v(TAG, "blockserver request " + request);

        client.newCall(request).enqueue(callback);
    }

    public void downloadFile(Context context, String prefix, String path, Callback callback) {
        doServerAction(context, prefix, path, "GET", null, callback);
    }

    public void uploadFile(Context context, String prefix, String path, byte[] data, Callback callback) {
        doServerAction(context, prefix, path, "POST", RequestBody.create(JSON, data), callback);
    }

    public void uploadFile(Context context, String prefix, String name, File file, Callback callback) {
        doServerAction(context, prefix, name, "POST", RequestBody.create(JSON, file), callback);

    }

    public void deleteFile(Context context, String prefix, String path, Callback callback) {
        doServerAction(context, prefix, path, "DELETE", null, callback);
    }

    public synchronized int getNextId() {
        return suffixId + currentId++ + (int) System.currentTimeMillis() % 1000000;
    }
}
