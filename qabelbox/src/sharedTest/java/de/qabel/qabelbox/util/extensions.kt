package de.qabel.qabelbox.util

import de.qabel.qabelbox.box.dto.DownloadSource
import de.qabel.qabelbox.box.dto.UploadSource
import rx.Observable
import rx.lang.kotlin.firstOrNull
import java.io.ByteArrayInputStream
import java.util.Date

fun String.toUploadSource() = UploadSource(this.toByteArrayInputStream(), this.length.toLong(), Date())
fun String.toByteArrayInputStream() = ByteArrayInputStream(this.toByteArray())
fun String.toDownloadSource() = DownloadSource(this.toByteArrayInputStream())

fun DownloadSource.asString() = source.reader().readText()
fun <T> Observable<T>.waitFor(): T = this.toBlocking().firstOrNull()
        ?: throw AssertionError("Got null from observable")
