package de.qabel.qabelbox.box.presenters

import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.provider.DocumentId

interface FolderChooserPresenter {

    fun enter(entry: BrowserEntry.Folder)

    fun selectFolder()

}

