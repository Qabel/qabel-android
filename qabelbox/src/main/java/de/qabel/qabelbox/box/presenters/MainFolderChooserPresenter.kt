package de.qabel.qabelbox.box.presenters

import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.interactor.FileBrowser
import de.qabel.qabelbox.box.views.FolderChooserView
import javax.inject.Inject

class MainFolderChooserPresenter @Inject constructor(
        val view: FolderChooserView,
        val browser: FileBrowser,
        navigatingPresenter: NavigatingPresenter = MainNavigatingPresenter(view, browser)
): FolderChooserPresenter, NavigatingPresenter by navigatingPresenter {

    override fun enter(entry: BrowserEntry.Folder) {
        path /= entry.name
        onRefresh()
    }

    override fun selectFolder() {
        browser.asDocumentId(path).subscribe { view.finish(it) }
    }

}

