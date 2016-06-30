package de.qabel.qabelbox.ui.presenters

import de.qabel.qabelbox.dto.BrowserEntry

interface FileBrowserPresenter {
    fun onRefresh()

    fun onClick(entry: BrowserEntry)
}

