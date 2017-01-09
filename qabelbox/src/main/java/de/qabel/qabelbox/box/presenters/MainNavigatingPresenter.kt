package de.qabel.qabelbox.box.presenters

import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.interactor.ReadFileBrowser
import de.qabel.qabelbox.box.views.FileListingView
import rx.Subscription
import javax.inject.Inject

open class MainNavigatingPresenter @Inject constructor(
        private val view: FileListingView,
        private val useCase: ReadFileBrowser
) : NavigatingPresenter {

    override var path: BoxPath.FolderLike = BoxPath.Root

    private var refreshSubscription: Subscription? = null

    override fun navigateUp(): Boolean {
        val isRoot = path is BoxPath.Root
        path = path.parent
        onRefresh()
        return !isRoot
    }

    override fun onRefresh() {
        view.refreshStart()
        view.backgroundRefreshStart()
        refreshSubscription?.apply {
            unsubscribe()
        }
        refreshSubscription = useCase.list(path, true).doOnCompleted {
            view.backgroundRefreshDone()
        }.subscribe({
            view.showEntries(it)
            view.refreshDone()
        }, { view.showError(it) })
    }


}

