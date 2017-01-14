package de.qabel.qabelbox.contacts.interactor

import de.qabel.core.config.Contacts

interface ReadOnlyContactsInteractor {
    fun findContacts(identityKeyId: String): Contacts
}

