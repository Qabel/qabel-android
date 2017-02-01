package de.qabel.qabelbox.box.views

import de.qabel.client.box.interactor.BrowserEntry

interface FileListingView {

    fun showEntries(entries: List<BrowserEntry>)
    fun refreshDone()
    fun refreshStart()
    fun showError(throwable: Throwable)
    fun backgroundRefreshStart()
    fun backgroundRefreshDone()

}

