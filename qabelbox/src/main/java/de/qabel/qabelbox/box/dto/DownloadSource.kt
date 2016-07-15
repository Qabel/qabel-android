package de.qabel.qabelbox.box.dto

import java.io.InputStream

data class DownloadSource(val entry: BrowserEntry.File, val source: InputStream)
