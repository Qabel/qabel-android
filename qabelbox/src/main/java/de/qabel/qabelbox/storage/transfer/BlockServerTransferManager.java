package de.qabel.qabelbox.storage.transfer;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.storage.server.AndroidBlockServer;
import de.qabel.qabelbox.communication.callbacks.DownloadRequestCallback;
import de.qabel.qabelbox.communication.callbacks.RequestCallback;
import de.qabel.qabelbox.communication.callbacks.UploadRequestCallback;
import de.qabel.qabelbox.storage.server.BlockServer;
import okhttp3.Response;

public class BlockServerTransferManager implements TransferManager {

    private static final Logger logger = LoggerFactory.getLogger(BlockServerTransferManager.class.getName());
    private static final String TAG = "TransferManager";
    private final File tempDir;
    private final Map<Integer, CountDownLatch> latches;
    private final Map<Integer, Exception> errors;
    private final BlockServer blockServer;
    private final Context context;

    public BlockServerTransferManager(Context context, BlockServer blockServer, File tmpDir){
        this.tempDir = tmpDir;
        latches = new ConcurrentHashMap<>();
        errors = new HashMap<>();

        this.context = context;
        this.blockServer = blockServer;
    }

    @Override
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
    @Override
    public int uploadAndDeleteLocalfileOnSuccess(String prefix, final String name, final File localfile, @Nullable final BoxTransferListener boxTransferListener) {

        Log.d(TAG, "uploadAndDeleteLocalfile " + prefix + " " + name + " " + localfile.toString());
        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.uploadFile(prefix, name, localfile, new UploadRequestCallback(new int[]{201, 204}) {

            @Override
            public void onProgress(long currentBytes, long totalBytes) {
                if (boxTransferListener != null) {
                    boxTransferListener.onProgressChanged(currentBytes, totalBytes);
                }
            }

            @Override
            protected void onSuccess(int statusCode, Response response) {
                Log.d(TAG, "uploadAndDeleteLocalfile response " + response.code());
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
                Log.d(TAG, "delete localfile " + localfile.getName());
                localfile.delete();
                latches.get(id).countDown();
            }

            @Override
            protected void onError(Exception e, @Nullable Response response) {
                errors.put(id, e);
                Log.e(TAG, "error uploading file " + name, e);
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
                latches.get(id).countDown();
            }
        });

        return id;
    }

    @Override
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
    @Override
    public int download(String prefix, String name, final File file, @Nullable final BoxTransferListener boxTransferListener) {

        Log.d(TAG, "download " + prefix + " " + name + " " + file.toString());

        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.downloadFile(prefix, name, new DownloadRequestCallback(file) {
            @Override
            public void onError(Exception e, @Nullable Response response) {
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
                errors.put(id, e);
                latches.get(id).countDown();
            }

            @Override
            protected void onProgress(long current, long size) {
                if (boxTransferListener != null) {
                    boxTransferListener.onProgressChanged(current, size);
                }
            }

            @Override
            public void onSuccess(int statusCode, Response response) {
                super.onSuccess(statusCode, response);
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
                latches.get(id).countDown();
            }
        });

        return id;
    }

    /**
     * wait until server request finished.
     *
     * @param id id (getted from up/downbload
     * @return true if no error occurs
     */
    @Override
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


    @Override
    public int delete(String prefix, String name) {
        Log.d(TAG, "delete " + prefix + " " + name);
        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.deleteFile(prefix, name, new RequestCallback(new int[]{200, 204, 404}) {
            @Override
            public void onError(Exception e, @Nullable Response response) {
                latches.get(id).countDown();
                errors.put(id, e);
            }

            @Override
            protected void onSuccess(int statusCode, Response response) {
                Log.d(TAG, "delete response " + response.code());
                latches.get(id).countDown();
            }
        });
        return id;
    }

}
