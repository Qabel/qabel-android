package de.qabel.qabelbox.box

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import de.qabel.box.storage.BoxFile
import de.qabel.box.storage.BoxFolder
import de.qabel.box.storage.BoxObject
import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.R
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.DownloadSource
import java.io.ByteArrayInputStream
import java.net.URLConnection
import java.util.*


fun ByteArray.toDownloadSource(entry: BrowserEntry.File)
        = DownloadSource(entry, ByteArrayInputStream(this))

fun ContentResolver.queryNameAndSize(uri: Uri): Pair<String, Long> =
            query(uri,
                  arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                  null, null, null).use {
                it.moveToFirst()
                Pair(it.getString(0), it.getLong(1))
            }

fun BoxObject.toEntry() = when (this) {
    is BoxFile -> BrowserEntry.File(this.name, this.size, Date(this.mtime))
    is BoxFolder -> BrowserEntry.Folder(this.name)
    else -> null
}

fun Uri.mimeType(): String {
    return URLConnection.guessContentTypeFromName(this.toString()) ?: "application/octet-stream"
}

fun Uri.openIntent(context: Context) {
    val uri = this
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        data = uri
        type = uri.mimeType()
        putExtra(Intent.EXTRA_SUBJECT, R.string.share_subject)
        putExtra(Intent.EXTRA_TITLE, R.string.share_subject)
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text))
        putExtra(Intent.EXTRA_STREAM, this@openIntent)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_via)))
}
