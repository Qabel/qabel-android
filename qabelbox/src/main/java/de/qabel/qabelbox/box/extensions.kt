package de.qabel.qabelbox.box

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.DownloadSource
import java.io.ByteArrayInputStream


fun ByteArray.toDownloadSource(entry: BrowserEntry.File)
        = DownloadSource(entry, ByteArrayInputStream(this))

fun ContentResolver.queryNameAndSize(uri: Uri): Pair<String, Long> =
            query(uri,
                  arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                  null, null, null).use {
                it.moveToFirst()
                Pair(it.getString(0), it.getLong(1))
            }
