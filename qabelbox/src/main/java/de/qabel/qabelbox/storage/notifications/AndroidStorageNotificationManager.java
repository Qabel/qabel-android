package de.qabel.qabelbox.storage.notifications;

import javax.inject.Inject;

import de.qabel.box.storage.BoxFile;
import de.qabel.qabelbox.storage.model.BoxUploadingFile;

public class AndroidStorageNotificationManager implements StorageNotificationManager {

    private StorageNotificationPresenter presenter;
    private BoxUploadingFile lastUpload;

    @Inject
    public AndroidStorageNotificationManager(StorageNotificationPresenter storageNotificationPresenter) {
        this.presenter = storageNotificationPresenter;
    }

    @Override
    public void updateUploadNotification(int queueSize, BoxUploadingFile currentUpload) {
        BoxUploadingFile displayFile;
        if (currentUpload != null) {
            this.lastUpload = currentUpload;
            displayFile = currentUpload;
        } else {
            displayFile = lastUpload;
        }
        presenter.updateUploadNotification(queueSize,
                new StorageNotificationInfo(displayFile.getName(), displayFile.getPath(),
                        displayFile.getOwnerIdentifier(), displayFile.uploadedSize,
                        displayFile.totalSize));
    }

    @Override
    public BoxTransferListener addDownloadNotification(final String ownerKey, final String path, final BoxFile file) {
        return new BoxTransferListener() {

            private StorageNotificationInfo notificationInfo =
                    new StorageNotificationInfo(file.getName(), path, ownerKey, 0, file.getSize());

            @Override
            public void onProgressChanged(long bytesCurrent, long bytesTotal) {
                notificationInfo.setProgress(bytesCurrent, bytesTotal);
                presenter.updateDownloadNotification(notificationInfo);
            }

            @Override
            public void onFinished() {
                notificationInfo.complete();
                presenter.updateDownloadNotification(notificationInfo);
            }
        };
    }
}
