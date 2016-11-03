package de.qabel.qabelbox.box.views

import de.qabel.qabelbox.box.dto.BrowserEntry

interface FileListingView {
    fun showEntries(entries: List<BrowserEntry>)
    fun refreshDone()
    fun refreshStart()
    fun showError(throwable: Throwable)
}

