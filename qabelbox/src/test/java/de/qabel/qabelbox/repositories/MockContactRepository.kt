package de.qabel.qabelbox.repositories

import de.qabel.core.config.Contact
import de.qabel.core.config.Contacts
import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.desktop.repository.ContactRepository
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion

class MockContactRepository(val contact: Contact): ContactRepository {
    override fun find(identities: Identities?, searchString : String?): MutableCollection<Pair<Contact, MutableList<Identity>>>? {
        throw UnsupportedOperationException()
    }

    override fun find(identity: Identity?): Contacts? {
        throw UnsupportedOperationException()
    }

    override fun save(contact: Contact?, identity: Identity?) {
        throw UnsupportedOperationException()
    }

    override fun delete(contact: Contact?, identity: Identity?) {
        throw UnsupportedOperationException()
    }

    override fun findByKeyId(identity: Identity?, keyId: String?): Contact? =
            if(keyId == contact.keyIdentifier) { contact }
            else throw EntityNotFoundExcepion("Contact not the same as the injected one")

}
