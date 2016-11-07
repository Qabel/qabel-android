package de.qabel.qabelbox.box.notifications;

public interface StorageNotificationPresenter {

    void updateDownloadNotification(StorageNotificationInfo notificationInfo);

    void updateUploadNotification(StorageNotificationInfo info);

}
