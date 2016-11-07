package de.qabel.qabelbox.box.notifications

import de.qabel.qabelbox.box.dto.FileOperationState
import javax.inject.Inject

class AndroidStorageNotificationManager
@Inject constructor(private val presenter: StorageNotificationPresenter) : StorageNotificationManager {

    override fun updateUploadNotification(fileOperationState: FileOperationState) =
            with(fileOperationState) {
                presenter.updateUploadNotification(StorageNotificationInfo(entryName,
                        path.toString(), ownerKey.publicKey,
                        done, size, completed))
            }

}
