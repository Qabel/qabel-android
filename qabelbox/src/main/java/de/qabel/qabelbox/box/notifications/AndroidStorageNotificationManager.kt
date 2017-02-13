package de.qabel.qabelbox.box.notifications

import de.qabel.client.box.interactor.FileOperationState
import de.qabel.client.box.interactor.FileOperationState.Status
import javax.inject.Inject

class AndroidStorageNotificationManager
@Inject constructor(private val presenter: StorageNotificationPresenter) : StorageNotificationManager {

    override fun updateUploadNotification(fileOperationState: FileOperationState) {
        val info = fileOperationState.toNotificationInfo()
        when (fileOperationState.status) {
            Status.PREPARE -> presenter.showEncryptingUploadNotification(info)
            Status.LOADING,
            Status.COMPLETING -> presenter.showUploadProgressNotification(info)
            Status.COMPLETE -> presenter.showUploadCompletedNotification(info)
            Status.ERROR -> presenter.showUploadFailedNotification(info)
            Status.HIDDEN,
            Status.CANCELED -> presenter.cancelNotification(info)
        }
    }

    override fun updateDownloadNotification(fileOperationState: FileOperationState) {
        val info = fileOperationState.toNotificationInfo()
        when (fileOperationState.status) {
            Status.PREPARE,
            Status.LOADING -> presenter.showDownloadProgressNotification(info)
            Status.COMPLETING -> presenter.showDecryptingDownloadNotification(info)
            Status.COMPLETE -> presenter.showDownloadCompletedNotification(info)
            Status.ERROR -> presenter.showDownloadFailedNotification(info)
            Status.HIDDEN,
            Status.CANCELED -> presenter.cancelNotification(info)
        }
    }

    private fun FileOperationState.toNotificationInfo(): StorageNotificationInfo =
            StorageNotificationInfo(entryName, path.toString(), ownerKey.publicKey,
                    time, done, size)

}
