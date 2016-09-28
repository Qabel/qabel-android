package de.qabel.qabelbox.index.view.views

import de.qabel.qabelbox.contacts.dto.ContactDto
import rx.Observable

interface IndexSearchView {

    var searchString : Observable<String>

    fun loadData(data: List<ContactDto>)
    fun showEmpty()
    fun showError(error: Throwable)

}

