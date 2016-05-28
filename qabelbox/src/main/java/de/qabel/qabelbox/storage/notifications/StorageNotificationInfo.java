package de.qabel.qabelbox.storage.notifications;

import android.support.annotation.NonNull;

import de.qabel.qabelbox.notifications.QblNotificationInfo;

public class StorageNotificationInfo implements QblNotificationInfo {

    protected static final int DOWNLOAD = 1;
    protected static final int UPLOAD = 1;

    private int type;
    private String fileName;
    private String path;
    private String identityKeyId;

    private long doneBytes;
    private long totalBytes;

    public StorageNotificationInfo(int type,
                                   @NonNull String fileName,
                                   @NonNull String path,
                                   @NonNull String identityKeyId,
                                   long doneBytes,
                                   long totalBytes) {
        this.type = type;
        this.fileName = fileName;
        this.path = path;
        this.identityKeyId = identityKeyId;
        this.doneBytes = doneBytes;
        this.totalBytes = totalBytes;
    }

    @Override
    public String getIdentifier() {
        return this.type + path + fileName + identityKeyId;
    }

    public int getType() {
        return this.type;
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

    public long getDoneBytes() {
        return doneBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

}
