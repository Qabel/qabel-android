package de.qabel.qabelbox.storage.notifications;

public interface StorageNotificationPresenter {

    void updateDownloadNotification(StorageNotificationInfo notificationInfo);

    void updateUploadNotification(int queueSize, StorageNotificationInfo info);

}
