package de.qabel.qabelbox.box.notifications

import de.qabel.qabelbox.notifications.QblNotificationInfo

data class StorageNotificationInfo(val fileName: String,
                              val path: String,
                              val identityKeyId: String,
                              val time: Long,
                              var doneBytes: Long = 0,
                              var totalBytes: Long = 1) : QblNotificationInfo {

    override fun getIdentifier(): String {
        return identityKeyId + path + fileName
    }

    val progress: Int
        get() = if(totalBytes > 0) (100 * doneBytes / totalBytes).toInt() else 0

}
