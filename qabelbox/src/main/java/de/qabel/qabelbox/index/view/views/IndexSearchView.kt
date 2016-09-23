package de.qabel.qabelbox.index.view.views

import de.qabel.qabelbox.contacts.dto.ContactDto

interface IndexSearchView {

    var searchString : String?

    fun loadData(data: List<ContactDto>)
    fun showEmpty()
    fun showError(error: Throwable)
    fun showDetails(contactDto: ContactDto)
}

