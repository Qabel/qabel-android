package de.qabel.qabelbox.box.views

import de.qabel.qabelbox.dto.BrowserEntry

interface FileBrowserView {
    fun showEntries(entries: List<BrowserEntry>)

}

