package de.qabel.qabelbox.contacts.view.views

import de.qabel.qabelbox.contacts.dto.ContactDto

interface ContactDetailsView {

    var contactKeyId: String

    fun loadContact(contact: ContactDto)
}

