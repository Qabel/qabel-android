package de.qabel.qabelbox.box.presenters

import de.qabel.core.config.Contact
import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import java.io.InputStream

interface FileBrowserPresenter {

    var path : BoxPath.FolderLike

    fun onRefresh()

    fun onClick(entry: BrowserEntry)

    fun share(file: BrowserEntry.File)

    fun delete(file: BrowserEntry.File)

    fun export(file: BrowserEntry.File)

    fun deleteFolder(folder: BrowserEntry.Folder)

    fun createFolder(folder: BrowserEntry.Folder)

    fun upload(file: BrowserEntry.File, stream: InputStream)

    fun navigateUp(): Boolean

    fun shareToContact(entry: BrowserEntry.File, contact: Contact)

    fun unShareFile(entry: BrowserEntry.File)

}

