package de.qabel.qabelbox.box.dto

import java.io.ByteArrayInputStream
import java.io.InputStream

class DownloadSource(val source: InputStream)

fun ByteArray.toDownloadSource() = DownloadSource(ByteArrayInputStream(this))
