package de.qabel.qabelbox.box.presenters

import android.net.Uri
import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.BrowserEntry
import de.qabel.client.box.interactor.BrowserEntry.File
import de.qabel.client.box.interactor.BrowserEntry.Folder
import de.qabel.client.box.interactor.ReadFileBrowser
import de.qabel.client.box.interactor.Sharer
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.box.interactor.BoxServiceStarter
import de.qabel.qabelbox.box.views.FileBrowserView
import de.qabel.qabelbox.navigation.Navigator
import javax.inject.Inject

class MainFileBrowserPresenter @Inject constructor(
        private val view: FileBrowserView,
        private val useCase: ReadFileBrowser,
        private val sharer: Sharer,
        private val identity: Identity,
        private val navigator: Navigator,
        private val boxServiceStarter: BoxServiceStarter,
        navigatingPresenter: NavigatingPresenter = MainNavigatingPresenter(view, useCase)) :
        FileBrowserPresenter, NavigatingPresenter by navigatingPresenter {

    override fun share(file: File) {
        withDocumentId(file) {
            view.share(it)
        }
    }

    override fun upload(file: File, uri: Uri) {
        withDocumentId(file) {
            boxServiceStarter.startUpload(it, uri)
        }
    }

    override fun delete(file: File) = deleteEntry(file)

    private fun deleteEntry(entry: BrowserEntry) {
        withDocumentId(entry) {
            boxServiceStarter.startDelete(it)
        }
    }


    private fun withDocumentId(entry: BrowserEntry, callback: (DocumentId) -> Unit) {
        val target = if (entry is File) path * entry.name else path / entry.name
        useCase.asDocumentId(target).subscribe {
            callback(it)
        }
    }

    override fun export(file: File) {
        withDocumentId(file) {
            view.export(it)
        }
    }

    override fun startExport(exportId: DocumentId, uri: Uri) {
        boxServiceStarter.startDownload(exportId, uri)
    }

    override fun deleteFolder(folder: Folder) = deleteEntry(folder)

    override fun createFolder(folder: Folder) {
        useCase.asDocumentId(path / folder.name).subscribe {
            boxServiceStarter.startCreateFolder(it)
        }
    }

    override fun onClick(entry: BrowserEntry) {
        when (entry) {
            is Folder -> {
                path /= entry.name
                onRefresh()
            }
            is File -> {
                withDocumentId(entry) {
                    view.open(it)
                }
            }

        }
    }

    override fun shareToContact(entry: File, contact: Contact) {
        view.refreshStart()
        sharer.sendFileShare(contact, path / entry.name).subscribe({
            navigator.selectContactChat(contact.keyIdentifier, identity)
        }, { view.showError(it) })
    }


    override fun unShareFile(entry: File) {
        view.refreshStart()
        sharer.revokeFileShare(path / entry.name).subscribe({
            onRefresh()
        }, {
            view.showError(it)
            onRefresh()
        })
    }

}

