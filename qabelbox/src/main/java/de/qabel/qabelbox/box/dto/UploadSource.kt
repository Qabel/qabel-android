package de.qabel.qabelbox.box.dto

import java.io.InputStream

data class UploadSource(val source: InputStream, val entry: BrowserEntry.File)

