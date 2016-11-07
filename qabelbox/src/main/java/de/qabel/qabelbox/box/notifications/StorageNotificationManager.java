package de.qabel.qabelbox.box.notifications;

import de.qabel.qabelbox.box.dto.FileOperationState;

public interface StorageNotificationManager {

    void updateUploadNotification(FileOperationState fileOperationState);

}
