package de.qabel.qabelbox.box.presenters

import android.net.Uri
import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.BrowserEntry
import de.qabel.core.config.Contact
import java.io.InputStream

interface FileBrowserPresenter: NavigatingPresenter {
    fun onClick(entry: BrowserEntry)

    fun share(file: BrowserEntry.File)

    fun delete(file: BrowserEntry.File)

    fun export(file: BrowserEntry.File)

    fun deleteFolder(folder: BrowserEntry.Folder)

    fun createFolder(folder: BrowserEntry.Folder)

    fun shareToContact(entry: BrowserEntry.File, contact: Contact)

    fun unShareFile(entry: BrowserEntry.File)

    fun upload(file: BrowserEntry.File, uri: Uri)

    fun startExport(exportId: DocumentId, uri: Uri)

}

