package de.qabel.qabelbox.storage.notifications;

import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxTransferListener;
import de.qabel.qabelbox.storage.BoxUploadingFile;

public interface StorageNotificationManager {

    void updateUploadNotification(int queueSize, BoxUploadingFile uploadingFile);

    BoxTransferListener addDownloadNotification(final String ownerKey, final String path, final BoxFile file);

}
