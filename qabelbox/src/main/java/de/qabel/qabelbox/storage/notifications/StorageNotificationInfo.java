package de.qabel.qabelbox.storage.notifications;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.qabel.qabelbox.notifications.QblNotificationInfo;

public class StorageNotificationInfo implements QblNotificationInfo {

    private String fileName;
    private String path;
    private String identityKeyId;

    private long doneBytes;
    private long totalBytes;

    public StorageNotificationInfo(@NonNull String fileName,
                                   @NonNull String path,
                                   @NonNull String identityKeyId,
                                   long doneBytes,
                                   long totalBytes) {
        this.fileName = fileName;
        this.path = path;
        this.identityKeyId = identityKeyId;
        this.doneBytes = doneBytes;
        this.totalBytes = totalBytes;
    }

    @Override
    public String getIdentifier() {
        return identityKeyId + path + fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPath() {
        return path;
    }

    public String getIdentityKeyId() {
        return identityKeyId;
    }

    public void setProgress(long doneBytes, long totalBytes) {
        this.doneBytes = doneBytes;
        this.totalBytes = totalBytes;
    }

    public long getDoneBytes() {
        return doneBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getProgress() {
        return (int) (100 * doneBytes / totalBytes);
    }

    public void complete(){
        totalBytes = doneBytes;
    }
    public boolean isComplete() {
        return doneBytes == totalBytes;
    }

}
