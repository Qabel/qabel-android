package de.qabel.qabelbox.box.presenters

import android.content.Context
import android.net.Uri
import de.qabel.core.config.Contact
import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.views.FileListingView
import java.io.InputStream

interface FileBrowserPresenter: NavigatingPresenter {
    fun onClick(entry: BrowserEntry)

    fun share(file: BrowserEntry.File)

    fun delete(file: BrowserEntry.File)

    fun export(file: BrowserEntry.File)

    fun deleteFolder(folder: BrowserEntry.Folder)

    fun createFolder(folder: BrowserEntry.Folder)

    fun upload(file: BrowserEntry.File, stream: InputStream)

    fun shareToContact(entry: BrowserEntry.File, contact: Contact)

    fun unShareFile(entry: BrowserEntry.File)

    fun upload(context: Context, file: BrowserEntry.File, uri: Uri)
}

