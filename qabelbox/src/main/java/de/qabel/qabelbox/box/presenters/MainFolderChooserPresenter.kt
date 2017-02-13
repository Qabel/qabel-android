package de.qabel.qabelbox.box.presenters

import de.qabel.client.box.interactor.BrowserEntry
import de.qabel.client.box.interactor.ReadFileBrowser
import de.qabel.qabelbox.box.views.FolderChooserView
import javax.inject.Inject

class MainFolderChooserPresenter @Inject constructor(
        val view: FolderChooserView,
        val browser: ReadFileBrowser,
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

