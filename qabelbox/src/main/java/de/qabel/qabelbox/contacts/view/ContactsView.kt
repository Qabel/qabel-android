package de.qabel.qabelbox.contacts.view

import de.qabel.qabelbox.contacts.dto.ContactDto


interface ContactsView {

    fun showEmpty()

    open fun loadData(data : List<ContactDto>)

}

