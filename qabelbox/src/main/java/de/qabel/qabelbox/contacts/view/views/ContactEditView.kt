package de.qabel.qabelbox.contacts.view.views

import de.qabel.core.config.Identities
import de.qabel.qabelbox.contacts.dto.ContactDto

interface ContactEditView {

    var contactKeyId: String

    fun getEditLabel() : String
    fun getNewLabel() : String

    fun loadContact(contact: ContactDto, identities: Identities)

    fun getCurrentNick(): String
    fun getCurrentIdentityIds(): List<Int>
    fun isContactIgnored(): Boolean

    fun showEnterNameToast()

    fun showContactSavedToast()
    fun showSaveFailed()

}

