package de.qabel.qabelbox.box.presenters

import de.qabel.client.box.interactor.BrowserEntry

interface FolderChooserPresenter: NavigatingPresenter {

    fun enter(entry: BrowserEntry.Folder)

    fun selectFolder()

}

