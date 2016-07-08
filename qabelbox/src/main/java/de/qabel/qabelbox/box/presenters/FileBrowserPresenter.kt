package de.qabel.qabelbox.box.presenters

import de.qabel.qabelbox.dto.BrowserEntry

interface FileBrowserPresenter {
    fun onRefresh()

    fun onClick(entry: BrowserEntry)

    fun open(file: BrowserEntry.File)

    fun share(file: BrowserEntry.File)

    fun delete(file: BrowserEntry.File)

    fun export(file: BrowserEntry.File)

    fun deleteFolder(folder: BrowserEntry.Folder)
}

