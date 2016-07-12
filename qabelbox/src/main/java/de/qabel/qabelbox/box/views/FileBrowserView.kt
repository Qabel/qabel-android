package de.qabel.qabelbox.box.views

import de.qabel.qabelbox.box.dto.BrowserEntry

interface FileBrowserView {
    fun showEntries(entries: List<BrowserEntry>)

}

