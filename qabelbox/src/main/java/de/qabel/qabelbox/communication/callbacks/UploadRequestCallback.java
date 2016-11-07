package de.qabel.qabelbox.communication.callbacks;

public abstract class UploadRequestCallback extends RequestCallback {

    public UploadRequestCallback(int... acceptedStatusCodes) {
        super(acceptedStatusCodes);
    }

}
