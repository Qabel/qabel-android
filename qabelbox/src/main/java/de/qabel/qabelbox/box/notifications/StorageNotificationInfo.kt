package de.qabel.qabelbox.box.notifications

import de.qabel.qabelbox.notifications.QblNotificationInfo

class StorageNotificationInfo(val fileName: String,
                              val path: String,
                              val identityKeyId: String,
                              var doneBytes: Long = 0,
                              var totalBytes: Long = 0,
                              var complete: Boolean = false) : QblNotificationInfo {

    override fun getIdentifier(): String {
        return identityKeyId + path + fileName
    }

    fun setProgress(doneBytes: Long, totalBytes: Long) {
        this.doneBytes = doneBytes
        this.totalBytes = totalBytes
    }

    val progress: Int
        get() = (100 * doneBytes / totalBytes).toInt()

}
