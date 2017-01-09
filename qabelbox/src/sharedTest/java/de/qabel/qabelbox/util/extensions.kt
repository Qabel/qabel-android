package de.qabel.qabelbox.util

import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.DownloadSource
import de.qabel.qabelbox.box.dto.UploadSource
import rx.Observable
import rx.lang.kotlin.firstOrNull
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

fun String.toUploadSource(entry: BrowserEntry.File)
        = UploadSource(this.toByteArrayInputStream(), entry)

fun String.toByteArrayInputStream() = ByteArrayInputStream(this.toByteArray())
fun String.toDownloadSource(file: BrowserEntry.File)
        = DownloadSource(file, this.toByteArrayInputStream())

fun DownloadSource.asString() = source.reader().readText()
fun <T> Observable<T>.waitFor(): T = try {
    this.toBlocking().last()
} catch (ex: NoSuchElementException) {
    throw AssertionError("Got null from observable")
}

fun InputStream.asString() = this.reader().readText()
