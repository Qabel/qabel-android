package de.qabel.qabelbox.ui.views

import de.qabel.qabelbox.dto.BrowserEntry

interface FileBrowserView {
    fun showEntries(entries: List<BrowserEntry>)

}

