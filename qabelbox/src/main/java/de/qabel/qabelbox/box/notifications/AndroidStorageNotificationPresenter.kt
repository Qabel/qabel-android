package de.qabel.qabelbox.box.notifications


import android.app.Notification
import android.content.Context
import android.content.Intent
import android.support.v7.app.NotificationCompat
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.MainActivity
import de.qabel.qabelbox.notifications.QblNotificationPresenter
import org.apache.commons.io.FileUtils

class AndroidStorageNotificationPresenter(context: Context) :
        QblNotificationPresenter<String, StorageNotificationInfo>(context), StorageNotificationPresenter {

    companion object {
        private val TAG = AndroidStorageNotificationPresenter::class.java.simpleName
        private const val UPLOAD_ICON = R.drawable.cloud_upload
        private const val DOWNLOAD_ICON = R.drawable.download
    }

    override fun getTag(): String = TAG

    fun createFileBrowserIntent(info: StorageNotificationInfo): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.ACTIVE_IDENTITY, info.identityKeyId)
                putExtra(MainActivity.START_FILES_FRAGMENT, true)
                putExtra(MainActivity.START_FILES_FRAGMENT_PATH, info.path)
            }

    private fun showNotification(info: StorageNotificationInfo, builder: NotificationCompat.Builder,
                                 autoCancel: Boolean = false) {
        builder.setWhen(info.time)
        builder.setAutoCancel(autoCancel)
        notify(getIdForInfo(info), builder, Notification.CATEGORY_PROGRESS, autoCancel)
    }

    private fun formatProgress(info: StorageNotificationInfo): String =
            "${FileUtils.byteCountToDisplaySize(info.doneBytes)} / " +
                    "${FileUtils.byteCountToDisplaySize(info.totalBytes)}\t ${info.progress}%"

    override fun showEncryptingUploadNotification(info: StorageNotificationInfo) {
        createNotification(createFileBrowserIntent(info),
                getString(R.string.uploading, info.fileName), UPLOAD_ICON,
                getString(R.string.encrypting)).let {
            it.setProgress(10, 10, true)
            showNotification(info, it, false)
        }
    }

    override fun showUploadProgressNotification(info: StorageNotificationInfo) {
        //TODO Cancel action
        createNotification(createFileBrowserIntent(info),
                getString(R.string.uploading, info.fileName), UPLOAD_ICON,
                formatProgress(info)).let {
            it.setProgress(100, info.progress, false)
            showNotification(info, it, false)
        }
    }

    override fun showUploadCompletedNotification(info: StorageNotificationInfo) {
        createNotification(createFileBrowserIntent(info),
                getString(R.string.upload_complete_title),
                UPLOAD_ICON,
                getString(R.string.upload_complete_msg, info.fileName)).let {
            showNotification(info, it, true)
        }
    }

    override fun showUploadFailedNotification(info: StorageNotificationInfo) {
        //TODO Retry action
        createNotification(createFileBrowserIntent(info),
                getString(R.string.upload_failed_title),
                UPLOAD_ICON,
                getString(R.string.upload_failed_msg, info.fileName)).let {
            showNotification(info, it, true)
        }
    }

    override fun showDownloadProgressNotification(info: StorageNotificationInfo) {
        //TODO Cancel action
        createNotification(createFileBrowserIntent(info),
                getString(R.string.downloading, info.fileName), DOWNLOAD_ICON,
                formatProgress(info)).let {
            it.setProgress(100, info.progress, false)
            showNotification(info, it, false)
        }
    }

    override fun showDecryptingDownloadNotification(info: StorageNotificationInfo) {
        createNotification(createFileBrowserIntent(info),
                getString(R.string.downloading, info.fileName), DOWNLOAD_ICON,
                getString(R.string.decrypting)).let {
            it.setProgress(10, 10, true)
            showNotification(info, it, false)
        }
    }

    override fun showDownloadCompletedNotification(info: StorageNotificationInfo) {
        createNotification(createFileBrowserIntent(info),
                getString(R.string.download_complete),
                UPLOAD_ICON,
                getString(R.string.download_complete_msg, info.fileName)).let {
            showNotification(info, it, true)
        }
    }

    override fun showDownloadFailedNotification(info: StorageNotificationInfo) {
        //TODO Retry action
        createNotification(createFileBrowserIntent(info),
                getString(R.string.download_failed), DOWNLOAD_ICON,
                getString(R.string.download_failed_msg, info.fileName)).let {
            showNotification(info, it, true)
        }
    }
}
