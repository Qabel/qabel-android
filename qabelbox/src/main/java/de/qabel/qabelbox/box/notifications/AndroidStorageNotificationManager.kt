package de.qabel.qabelbox.box.notifications

import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.dto.FileOperationState.Status
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
        }
    }

    private fun FileOperationState.toNotificationInfo(): StorageNotificationInfo =
            StorageNotificationInfo(entryName, path.toString(), ownerKey.publicKey,
                    time, done, size)

}
