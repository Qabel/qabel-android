package de.qabel.qabelbox.communication.callbacks;

public abstract class UploadRequestCallback extends RequestCallback {

    public UploadRequestCallback(int... acceptedStatusCodes) {
        super(acceptedStatusCodes);
    }

    public abstract void onProgress(long currentBytes, long totalBytes);
}
