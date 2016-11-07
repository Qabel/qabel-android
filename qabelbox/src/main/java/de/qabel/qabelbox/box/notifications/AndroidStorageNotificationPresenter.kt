package de.qabel.qabelbox.box.notifications


import android.app.Notification
import android.content.Context
import android.content.Intent
import android.support.v7.app.NotificationCompat

import org.apache.commons.io.FileUtils

import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.MainActivity
import de.qabel.qabelbox.notifications.QblNotificationPresenter

class AndroidStorageNotificationPresenter(context: Context) :
        QblNotificationPresenter<String, StorageNotificationInfo>(context), StorageNotificationPresenter {

    override fun getTag(): String {
        return TAG
    }

    fun createFileIntent(info: StorageNotificationInfo): Intent {
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        notificationIntent.putExtra(MainActivity.ACTIVE_IDENTITY, info.identityKeyId)
        notificationIntent.putExtra(MainActivity.START_FILES_FRAGMENT, true)
        notificationIntent.putExtra(MainActivity.START_FILES_FRAGMENT_PATH, info.path)
        return notificationIntent
    }

    override fun updateUploadNotification(info: StorageNotificationInfo) {
        val notificationId = getIdForInfo(info)
        val fileIntent = createFileIntent(info)
        val notificationBuilder: NotificationCompat.Builder
        if (!info.complete) {
            val title = context.getString(R.string.uploading, info.fileName)
            val content = if (info.doneBytes == 0L) {
                //Show encrypting label if upload not started
                context.getString(R.string.encrypting)
            } else {
                formatProgress(info)
            }
            notificationBuilder = createNotification(
                    fileIntent, title, R.drawable.cloud_upload, content)
            notificationBuilder.setProgress(100, info.progress, false)
            notificationBuilder.setAutoCancel(false)
        } else {
            notificationBuilder = createNotification(
                    fileIntent,
                    String.format(getString(R.string.upload_complete_notification_title)),
                    R.drawable.cloud_upload,
                    String.format(getString(R.string.upload_complete_notification_msg),
                            info.fileName))
        }
        showNotification(notificationId, notificationBuilder)
    }

    private fun showNotification(id: Int, builder: NotificationCompat.Builder) {
        notify(id, builder, Notification.CATEGORY_PROGRESS, false)
    }

    override fun updateDownloadNotification(info: StorageNotificationInfo) {
        val fileIntent = createFileIntent(info)
        val title: String
        val msg: String
        if (!info.complete) {
            title = String.format(getString(R.string.downloading), info.fileName)
            msg = if (info.progress < 100) {
                formatProgress(info)
            } else {
                context.getString(R.string.decrypting)
            }
        } else {
            title = getString(R.string.download_complete)
            msg = String.format(getString(R.string.download_complete_msg), info.fileName)
        }
        val notificationBuilder = createNotification(fileIntent, title, R.drawable.download, msg)

        if (!info.complete) {
            notificationBuilder.setProgress(100, info.progress, false)
        }
        showNotification(getIdForInfo(info), notificationBuilder)
    }

    private fun formatProgress(info: StorageNotificationInfo): String =
            "${FileUtils.byteCountToDisplaySize(info.doneBytes)} / " +
                    "${FileUtils.byteCountToDisplaySize(info.totalBytes)}\t ${info.progress}%"

    companion object {
        private val TAG = AndroidStorageNotificationPresenter::class.java.simpleName
    }
}
