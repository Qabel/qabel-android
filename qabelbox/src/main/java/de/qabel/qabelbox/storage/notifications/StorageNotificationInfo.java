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

    public void complete() {
        totalBytes = doneBytes;
    }

    public boolean isComplete() {
        return doneBytes == totalBytes;
    }

    @Override
    public String toString() {
        return "StorageNotifcationInfo{" +
                "file='" + fileName + '\'' +
                ", path='" + path + '\'' +
                ", owner='" + identityKeyId + '\'' +
                ", status " + doneBytes + "/" + totalBytes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StorageNotificationInfo)) return false;

        StorageNotificationInfo that = (StorageNotificationInfo) o;

        if (!fileName.equals(that.fileName) ||
                !path.equals(that.path) ||
                !identityKeyId.equals(that.identityKeyId) ||
                doneBytes != that.doneBytes ||
                totalBytes != that.totalBytes) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fileName.hashCode();
        result = 31 * result + path.hashCode();
        result = 31 * result + identityKeyId.hashCode();
        result = 31 * result + new Long(doneBytes).intValue();
        result = 31 * result + new Long(totalBytes).intValue();
        return result;
    }
}
