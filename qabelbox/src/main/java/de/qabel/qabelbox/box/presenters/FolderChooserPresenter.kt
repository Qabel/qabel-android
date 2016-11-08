package de.qabel.qabelbox.box.presenters

import de.qabel.qabelbox.box.dto.BrowserEntry

interface FolderChooserPresenter: NavigatingPresenter {

    fun enter(entry: BrowserEntry.Folder)

    fun selectFolder()

}

