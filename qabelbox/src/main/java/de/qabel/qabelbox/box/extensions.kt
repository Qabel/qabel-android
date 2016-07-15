package de.qabel.qabelbox.box

import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.DownloadSource
import java.io.ByteArrayInputStream


fun ByteArray.toDownloadSource(entry: BrowserEntry.File)
        = DownloadSource(entry, ByteArrayInputStream(this))
