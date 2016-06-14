package de.qabel.qabelbox.storage.notifications;

import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.transfer.BoxTransferListener;
import de.qabel.qabelbox.storage.model.BoxUploadingFile;

public interface StorageNotificationManager {

    void updateUploadNotification(int queueSize, BoxUploadingFile uploadingFile);

    BoxTransferListener addDownloadNotification(final String ownerKey, final String path, final BoxFile file);

}
