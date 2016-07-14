package de.qabel.qabelbox.box

import de.qabel.qabelbox.box.dto.DownloadSource
import java.io.ByteArrayInputStream


fun ByteArray.toDownloadSource() = DownloadSource(ByteArrayInputStream(this))
