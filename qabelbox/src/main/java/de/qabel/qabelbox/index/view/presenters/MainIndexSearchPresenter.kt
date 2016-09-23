package de.qabel.qabelbox.index.view.presenters

import de.qabel.qabelbox.index.interactor.IndexSearchUseCase
import de.qabel.qabelbox.index.view.views.IndexSearchView

class MainIndexSearchPresenter(val view: IndexSearchView, val useCase: IndexSearchUseCase)
: IndexSearchPresenter {
    override fun search(email: String, phone: String) {
        useCase.search(email, phone).subscribe {
            if (it.size > 0) { view.loadData(it) } else { view.showEmpty() }
        }
    }

}
