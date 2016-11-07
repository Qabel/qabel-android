package de.qabel.qabelbox.box.presenters

import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.interactor.FileBrowser
import de.qabel.qabelbox.box.views.FileListingView
import javax.inject.Inject

open class MainNavigatingPresenter @Inject constructor(
        private val view: FileListingView,
        private val useCase: FileBrowser
): NavigatingPresenter {

    override var path: BoxPath.FolderLike = BoxPath.Root

    override fun navigateUp(): Boolean {
        val isRoot = path is BoxPath.Root
        path = path.parent
        onRefresh()
        return !isRoot
    }

    override fun onRefresh() {
        view.refreshStart()
        useCase.list(path).subscribe({
            view.showEntries(it)
            view.refreshDone()
        }, { view.showError(it) })
    }


}

