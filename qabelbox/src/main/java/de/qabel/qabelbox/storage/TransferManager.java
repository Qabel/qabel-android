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
import de.qabel.qabelbox.exceptions.QblServerException;
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

    public File createTempFile() {

        try {
            return File.createTempFile("download", "", tempDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create tempfile");
        }
    }

    /**
     * uploadAndDeleteLocalfile localfile to server
     * For convience the localfile will be delete after the oparation is finished
     *
     * @param prefix              prefix from identity
     * @param name                localfile name with path
     * @param localfile           localfile to uploadAndDeleteLocalfile
     * @param boxTransferListener listener
     * @return new download id
     */
    public int uploadAndDeleteLocalfileOnSuccess(String prefix, String name, final File localfile, @Nullable final BoxTransferListener boxTransferListener) {

        Log.d(TAG, "uploadAndDeleteLocalfile " + prefix + " " + name + " " + localfile.toString());
        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.uploadFile(context, prefix, name, localfile, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                errors.put(id, e);
                Log.e(TAG, "error uploading to " + call.request(), e);
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
                latches.get(id).countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {


                int status = response.code();
                switch (status) {
                    case 201:
                    case 204:
                        Log.d(TAG, "uploadAndDeleteLocalfile response " + response.code() + "(" + call.request() + ")");
                        if (boxTransferListener != null) {
                            boxTransferListener.onFinished();
                        }
                        Log.d(TAG, "delete localfile " + localfile.getName());
                        localfile.delete();
                        latches.get(id).countDown();
                        break;
                    default:
                        String msg = "Unexpected status code (" + response.code() + " for " + call.request();
                        errors.put(id, new QblServerException(response.code(), call.request().toString()));
                        Log.e(TAG, msg);
                        Log.e(TAG, response.message());
                        latches.get(id).countDown();
                }


            }
        });

        return id;
    }

    public Exception lookupError(int transferId) {
        return errors.get(transferId);
    }

    /**
     * download file from server
     *
     * @param prefix              prefix from identity
     * @param name                file name with directory
     * @param file                destination file
     * @param boxTransferListener listener
     * @return new download id
     */
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
                Log.d(TAG, "download response " + response.code() + " on " + call.request());
                switch (response.code()) {
                    case 200:
                        readStreamFromServer(response, file, boxTransferListener);
                        break;
                    default:
                        QblServerException genericError = new QblServerException(response.code(), call.request().toString());
                        errors.put(id, genericError);
                        Log.w(TAG, "download failure", genericError);
                        break;
                }
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
                latches.get(id).countDown();
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

    /**
     * wait until server request finished.
     *
     * @param id id (getted from up/downbload
     * @return true if no error occurs
     */
    public boolean waitFor(int id) {

        logger.info("Waiting for " + id);
        try {
            latches.get(id).await();
            logger.info("Waiting for " + id + " finished");
            Exception e = errors.get(id);
            if (e != null) {
                logger.warn("Error found waiting for " + id, e);
            }
            return e == null;
        } catch (InterruptedException e) {
            return false;
        }
    }


    public int delete(String prefix, String name) {
        Log.d(TAG, "delete " + prefix + " " + name);
        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.deleteFile(context, prefix, name, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                latches.get(id).countDown();
                errors.put(id, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "delete response " + response.code());
                latches.get(id).countDown();
                switch (response.code()) {
                    case 200:
                    case 204:
                    case 404: // 404 can be safely ignored
                        break;
                    default:
                        QblServerException genericError = new QblServerException(response.code(), call.request().toString());
                        Log.w(TAG, genericError.getMessage());
                        errors.put(id, genericError);
                        break;
                }
            }
        });
        return id;
    }

    public interface BoxTransferListener {

        void onProgressChanged(long bytesCurrent, long bytesTotal);

        void onFinished();
    }
}
