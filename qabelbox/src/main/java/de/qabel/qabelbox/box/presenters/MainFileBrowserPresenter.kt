package de.qabel.qabelbox.box.presenters

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.BrowserEntry.File
import de.qabel.qabelbox.box.dto.BrowserEntry.Folder
import de.qabel.qabelbox.box.dto.UploadSource
import de.qabel.qabelbox.box.interactor.FileBrowser
import de.qabel.qabelbox.box.interactor.Sharer
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.views.FileBrowserView
import de.qabel.qabelbox.navigation.Navigator
import java.io.InputStream
import javax.inject.Inject

class MainFileBrowserPresenter @Inject constructor(
        private val view: FileBrowserView,
        private val useCase: FileBrowser,
        private val sharer: Sharer,
        private val identity: Identity,
        private val navigator: Navigator):
        FileBrowserPresenter {
    override fun navigateUp() {
        path = path.parent
        onRefresh()
    }

    var path: BoxPath.FolderLike = BoxPath.Root


    override fun share(file: File) {
        withDocumentId(file) {
            view.share(it)
        }
    }

    override fun upload(file: File, stream: InputStream) {
        view.refreshStart()
        useCase.upload(path * file.name, UploadSource(stream, file)).subscribe({
            onRefresh()
        }, { view.showError(it) })
    }

    override fun delete(file: File) {
        useCase.delete(BoxPath.Root * file.name).subscribe({
            onRefresh()
        }, { view.showError(it) })
    }


    private fun withDocumentId(file: File, callback: (DocumentId)  -> Unit) {
        useCase.asDocumentId(path * file.name).subscribe {
            callback(it)
        }
    }

    override fun export(file: File) {
        withDocumentId(file) {
            view.export(it)
        }
    }

    override fun deleteFolder(folder: Folder) {
        view.refreshStart()
        useCase.delete(path / folder.name).subscribe({
            onRefresh()
        }, { view.showError(it) })
    }

    override fun createFolder(folder: Folder) {
        view.refreshStart()
        useCase.createFolder(path / folder.name).subscribe({
            onRefresh()
        }, { view.showError(it) })
    }

    override fun onRefresh() {
        view.refreshStart()
        useCase.list(path).subscribe({
            view.showEntries(it)
            view.refreshDone()
        }, { view.showError(it) })
    }

    override fun onClick(entry: BrowserEntry) {
        when(entry) {
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
            view.refreshDone()
            navigator.selectContactChat(contact.keyIdentifier, identity)
        },{ view.showError(it)})
    }

}

