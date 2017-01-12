package de.qabel.qabelbox.box.notifications;

import de.qabel.client.box.interactor.FileOperationState;

public interface StorageNotificationManager {

    void updateUploadNotification(FileOperationState fileOperationState);

    void updateDownloadNotification(FileOperationState fileOperationState);

}
