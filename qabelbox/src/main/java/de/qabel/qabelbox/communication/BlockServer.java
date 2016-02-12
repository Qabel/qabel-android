package de.qabel.qabelbox.communication;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.FileHelper;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by danny on 11.02.2016.
 * <p/>
 * class to handle prefix server network action
 */
public class BlockServer extends BaseServer {

    private final static String TAG = "PrefixServer";

    protected void doServerAction(Context context, String prefix, String path, String method, RequestBody body, Callback callback) {

        String url = urls.getFiles() + prefix + path;
        Request.Builder builder = new Request.Builder()
                .url(url);

        builder = builder.method(method, body);

        addHeader(new AppPreference(context).getToken(), builder);
        Request request = builder.build();
        Log.d(TAG, "danny request " + request.toString());
        client.newCall(request).enqueue(callback);
    }

    public void downloadFile(Context context, String prefix, String path, Callback callback) {

        doServerAction(context, prefix, path, "GET", null, callback);
    }

    public void uploadFile(Context context, String prefix, String path, byte[] data, Callback callback) {

        doServerAction(context, prefix, path, "POST", RequestBody.create(JSON, data), callback);
    }

    public void deleteFile(Context context, String prefix, String path, Callback callback) {

        doServerAction(context, prefix, path, "DELETE", null, callback);
    }

    public void downloadFile(Context context, Identity identity, String path, Callback callback) {

        doServerAction(context, VolumeFileTransferHelper.getPrefixFromIdentity(identity), path, "GET", null, callback);
    }

    public void downloadFile(Context context, String path, Callback callback) {

        doServerAction(context, VolumeFileTransferHelper.getPrefixFromIdentity(QabelBoxApplication.getInstance().getService().getActiveIdentity()), path, "GET", null, callback);
    }

    public void uploadFile(Context context, Identity identity, String path, byte[] data, Callback callback) {

        doServerAction(context, VolumeFileTransferHelper.getPrefixFromIdentity(identity), path, "POST", RequestBody.create(JSON, data), callback);
    }

    public void deleteFile(Context context, Identity identity, String path, Callback callback) {

        doServerAction(context, VolumeFileTransferHelper.getPrefixFromIdentity(identity), path, "DELETE", null, callback);
    }

    public void uploadFile(Context context, String prefix, String name, File file, Callback callback) {

        byte[] data = new byte[0];
        try {
            FileInputStream fis = new FileInputStream(file);
            data = FileHelper.readFileAsBinary(fis);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        uploadFile(context, prefix, name, data, callback);
    }

    int currentId = 0;

    public int getNextId() {
        //short way. maybe this can doing bether
        return (this.getClass().hashCode() % 10000) + currentId++;
    }

    public void uploadFile(Context context, String name, File file, Callback callback) {

        uploadFile(context, VolumeFileTransferHelper.getPrefixFromIdentity(QabelBoxApplication.getInstance().getService().getActiveIdentity()), name, file, callback);
    }
}
