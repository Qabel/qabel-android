package de.qabel.qabelbox.storage.notifications;

public interface BoxTransferListener {
    void onProgressChanged(long bytesCurrent, long bytesTotal);

    void onFinished();
}
