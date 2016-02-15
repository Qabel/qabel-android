package de.qabel.qabelbox.storage;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.communication.BlockServer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TransferManager {

    private static final Logger logger = LoggerFactory.getLogger(TransferManager.class.getName());
    private static final String TAG = "TransferManager";
    private final File tempDir;
    private final Map<Integer, CountDownLatch> latches;
    private final Map<Integer, Exception> errors;
    private final BlockServer blockServer = new BlockServer();
    private final Context context;

    public TransferManager(File tempDir) {

        this.tempDir = tempDir;
        latches = new ConcurrentHashMap<>();
        errors = new HashMap<>();

        context = QabelBoxApplication.getInstance().getApplicationContext();
    }

    private String getKey(String prefix, String name) {

        return prefix + '/' + name;
    }

    public File createTempFile() {

        try {
            return File.createTempFile("download", "", tempDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create tempfile");
        }
    }

    public int upload(String prefix, String name, File file, @Nullable final BoxTransferListener boxTransferListener) {

        Log.d(TAG, "upload " + prefix + " " + name + " " + file.toString());
        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.uploadFile(context, prefix, name, file, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                errors.put(id, e);

                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
                latches.get(id).countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                latches.get(id).countDown();
                Log.d(TAG, "upload response " + response.code());
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
            }
        });

        return id;
    }

    public int download(String prefix, String name, final File file, @Nullable final BoxTransferListener boxTransferListener) {

        Log.d(TAG, "download " + prefix + " " + name + " " + file.toString());

        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.downloadFile(context, prefix, name, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
                errors.put(id, e);
                latches.get(id).countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Log.d(TAG, "download response " + response.code());
                if (response.code() == 200) {
                    readStreamFromServer(response, file, boxTransferListener);
                } else {
                    Log.d(TAG, "donwload failure");
                }
                latches.get(id).countDown();
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
            }
        });

        return id;
    }

    /**
     * read stream from server
     *
     * @param response
     * @param file
     * @param boxTransferListener
     * @throws IOException
     */
    private void readStreamFromServer(Response response, File file, @Nullable BoxTransferListener boxTransferListener) throws IOException {
        InputStream is = response.body().byteStream();
        BufferedInputStream input = new BufferedInputStream(is);
        OutputStream output = new FileOutputStream(file);

        Log.d(TAG, "Server response received. Reading stream with unknown size");
        final byte[] data = new byte[1024];
        long total = 0;
        int count;
        while ((count = input.read(data)) != -1) {
            total += count;
            output.write(data, 0, count);
        }

        Log.d(TAG, "download filesize after: " + total);
        if (boxTransferListener != null) {
            boxTransferListener.onProgressChanged(total, total);
        }
        output.flush();
        output.close();
        input.close();
    }

    public boolean waitFor(int id) {

        logger.info("Waiting for " + id);
        try {
            latches.get(id).await();
            logger.info("Waiting for " + id + " finished");
            Exception e = errors.get(id);
            return e == null;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void delete(String prefix, String ref) {

        //  awsClient.deleteObject(bucket, getKey(prefix, ref));
        //@todo delete ref, whats that?

        Log.d(TAG, "delete " + prefix + " " + ref);

        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.downloadFile(context, prefix, ref, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                latches.get(id).countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Log.d(TAG, "delete response " + response.code());

                latches.get(id).countDown();
            }
        });
    }

    public interface BoxTransferListener {

        void onProgressChanged(long bytesCurrent, long bytesTotal);

        void onFinished();
    }
}
