package de.qabel.qabelbox.box.presenters

import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.BrowserEntry.*
import de.qabel.qabelbox.box.dto.UploadSource
import de.qabel.qabelbox.box.interactor.FileBrowserUseCase
import de.qabel.qabelbox.box.views.FileBrowserView
import java.io.InputStream
import javax.inject.Inject

class MainFileBrowserPresenter @Inject constructor(
        private val view: FileBrowserView, private val useCase: FileBrowserUseCase):
        FileBrowserPresenter {

    var path: BoxPath.FolderLike = BoxPath.Root

    override fun open(file: File) {
        TODO()
    }

    override fun share(file: File) {
        TODO()
    }

    override fun upload(file: File, stream: InputStream) {
        useCase.upload(path * file.name, UploadSource(stream, file.size, file.mTime)).subscribe {
            onRefresh()
        }
    }

    override fun delete(file: File) {
        useCase.delete(BoxPath.Root * file.name).subscribe {
            onRefresh()
        }
    }

    override fun export(file: File) {
        TODO()
    }

    override fun deleteFolder(folder: Folder) {
        useCase.delete(path / folder.name).subscribe {
            onRefresh()
        }
    }

    override fun createFolder(folder: Folder) {
        useCase.createFolder(path / folder.name).subscribe {
            onRefresh()
        }
    }

    override fun onRefresh() {
        useCase.list(path).subscribe {
            view.showEntries(it)
        }
    }

    override fun onClick(entry: BrowserEntry) {
        if (entry is Folder) {
            path /= entry.name
            onRefresh()
        }
    }
}

