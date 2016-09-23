package de.qabel.qabelbox.index.view.presenters

import de.qabel.qabelbox.contacts.dto.ContactDto

interface IndexSearchPresenter {
    fun search()

    fun showDetails(contact: ContactDto)
}

