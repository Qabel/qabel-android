package de.qabel.qabelbox.storage.transfer;

public interface BoxTransferListener {

    void onProgressChanged(long bytesCurrent, long bytesTotal);

    void onFinished();

}
