package de.qabel.qabelbox.box.provider

import android.net.Uri
import android.provider.DocumentsContract
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.chat.repository.entities.BoxFileChatShare
import de.qabel.qabelbox.BuildConfig

/**
 * Share IDs are built like this:
 * SHARE::::boxShareId::::name
 */
data class ShareId(val boxShareId: Int, val name: String) {

    companion object {
        val PREFIX = "SHARE"
        fun parse(shareId: String): ShareId {
            val parts = shareId.split(BoxProvider.DOCID_SEPARATOR.toRegex(), 3)
            if (parts.size != 3 || parts[0] != PREFIX) {
                throw QblStorageException("Invalid shareId: " + shareId)
            }
            return ShareId(Integer.parseInt(parts[1]), parts[2])
        }

        fun create(share: BoxFileChatShare): ShareId = ShareId(share.id, share.name)

    }

    fun toUri(): Uri = DocumentsContract.buildDocumentUri(
            BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY,
            toString())

    override fun toString(): String =
            PREFIX + BoxProvider.DOCID_SEPARATOR +
                    boxShareId + BoxProvider.DOCID_SEPARATOR +
                    name

}
