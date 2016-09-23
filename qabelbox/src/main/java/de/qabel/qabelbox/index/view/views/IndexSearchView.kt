package de.qabel.qabelbox.index.view.views

import de.qabel.qabelbox.contacts.dto.ContactDto

interface IndexSearchView {
    fun loadData(data: List<ContactDto>)
    fun showEmpty()
    fun showDetails(contact: ContactDto)
}

