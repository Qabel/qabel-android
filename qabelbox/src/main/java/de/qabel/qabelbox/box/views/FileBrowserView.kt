package de.qabel.qabelbox.box.views

import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.DownloadSource

interface FileBrowserView {
    fun showEntries(entries: List<BrowserEntry>)
    fun open(file: BrowserEntry.File, source: DownloadSource)
    fun export(file: BrowserEntry.File, source: DownloadSource)
}

