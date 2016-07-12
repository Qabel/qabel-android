package de.qabel.qabelbox.box.presenters

import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.BrowserEntry.*
import de.qabel.qabelbox.box.interactor.FileBrowserUseCase
import de.qabel.qabelbox.box.views.FileBrowserView
import javax.inject.Inject

class MainFileBrowserPresenter @Inject constructor(
        private val view: FileBrowserView, private val useCase: FileBrowserUseCase):
        FileBrowserPresenter {

    override fun open(file: File) {
        TODO()
    }

    override fun share(file: File) {
        TODO()
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
        useCase.delete(BoxPath.Root / folder.name).subscribe {
            onRefresh()
        }
    }

    override fun onRefresh() {
        useCase.list(BoxPath.Root).subscribe {
            view.showEntries(it)
        }
    }

    override fun onClick(entry: BrowserEntry) = TODO()
}

