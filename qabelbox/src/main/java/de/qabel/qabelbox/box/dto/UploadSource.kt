package de.qabel.qabelbox.box.dto

import java.io.InputStream
import java.util.*

data class UploadSource(val source: InputStream, val size: Long, val mTime: Date)

