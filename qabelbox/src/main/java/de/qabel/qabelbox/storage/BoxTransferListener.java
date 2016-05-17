package de.qabel.qabelbox.storage;

public interface BoxTransferListener {

    void onProgressChanged(long bytesCurrent, long bytesTotal);

    void onFinished();
}
