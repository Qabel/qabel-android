package de.qabel.qabelbox.box.notifications

interface StorageNotificationPresenter {

    fun showEncryptingUploadNotification(info: StorageNotificationInfo)
    fun showUploadProgressNotification(info: StorageNotificationInfo)
    fun showUploadCompletedNotification(info: StorageNotificationInfo)
    fun showUploadFailedNotification(info: StorageNotificationInfo)

    fun showDownloadProgressNotification(info: StorageNotificationInfo)
    fun showDownloadCompletedNotification(info: StorageNotificationInfo)
    fun showDownloadFailedNotification(info: StorageNotificationInfo)
    fun showDecryptingDownloadNotification(info: StorageNotificationInfo)

}
