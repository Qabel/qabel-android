package de.qabel.core.storage;

import android.support.annotation.Nullable;

import com.amazonaws.mobileconnectors.s3.transferutility.*;
import com.amazonaws.services.s3.AmazonS3Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class TransferManager implements TransferListener {

    private static final Logger logger = LoggerFactory.getLogger(TransferManager.class.getName());

    private AmazonS3Client awsClient;
    private final String bucket;
    private String prefix;
    private File tempDir;
    private final TransferUtility transferUtility;

    private final Map<Integer, Semaphore> semaphores;
    private final Map<Integer, Exception> errors;
    private final Map<Integer, BoxTransferListener> transferListeners;

    public TransferManager(TransferUtility transferUtility, AmazonS3Client awsClient,
                           String bucket, String prefix, File tempDir) {
        this.transferUtility = transferUtility;
        this.awsClient = awsClient;
        this.bucket = bucket;
        this.prefix = prefix;
        this.tempDir = tempDir;
        semaphores = new ConcurrentHashMap<>();
        errors = new HashMap<>();
        transferListeners = new ConcurrentHashMap<>();
    }

    public interface BoxTransferListener {
        void onProgressChanged(long bytesCurrent, long bytesTotal);
        void onFinished();
    }

    private String getKey(String name) {
        return prefix+'/'+name;
    }

    public File createTempFile() {
        try {
            return File.createTempFile("download", "", tempDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create tempfile");
        }
    }


    public int upload(String name, File file, @Nullable BoxTransferListener boxTransferListener) {
        TransferObserver upload = transferUtility.upload(bucket, getKey(name), file);
        int id = upload.getId();
        logger.info("Uploading " + name + " id " + id);
        semaphores.put(id, new Semaphore(0));
        if (boxTransferListener != null) {
            transferListeners.put(id, boxTransferListener);
        }
        upload.setTransferListener(this);
        return upload.getId();
    }

    public int download(String name, File file, @Nullable BoxTransferListener boxTransferListener) {
        TransferObserver download = transferUtility.download(bucket, getKey(name), file);
        int id = download.getId();
        logger.info("Downloading " + name + " id " + id);
        semaphores.put(id, new Semaphore(0));
        if (boxTransferListener != null) {
            transferListeners.put(id, boxTransferListener);
        }
        download.setTransferListener(this);
        return id;
    }

    public boolean waitFor(int id) {
        logger.info("Waiting for " + id);
        try {
            semaphores.get(id).acquire();
            Exception e = errors.get(id);
            if (e != null) {
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public boolean cancel(int id) {
        return transferUtility.cancel(id);
    }

    @Override
    public void onStateChanged(int id, TransferState state) {
        logger.info("State changed " + id + ": " + state);
        if (state == TransferState.COMPLETED) {
            semaphores.get(id).release();
            BoxTransferListener boxTransferListener = transferListeners.get(id);
            if (boxTransferListener != null) {
                boxTransferListener.onFinished();
            }
        }
    }

    @Override
    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
        BoxTransferListener boxTransferListener = transferListeners.get(id);
        if (boxTransferListener != null) {
            boxTransferListener.onProgressChanged(bytesCurrent, bytesTotal);
        }
    }

    @Override
    public void onError(int id, Exception ex) {
        logger.error("Error for id " + id, ex);
        errors.put(id, ex);
        semaphores.get(id).release();
    }


    public void delete(String ref) {
        awsClient.deleteObject(bucket, getKey(ref));
    }
}
