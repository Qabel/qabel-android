package de.qabel.qabelbox.repositories

import de.qabel.core.config.Contact
import de.qabel.core.config.Contacts
import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.desktop.repository.ContactRepository
import de.qabel.desktop.repository.exception.EntityNotFoundException

class MockContactRepository(val contact: Contact): ContactRepository {

    override fun exists(contact: Contact?): Boolean? {
        throw UnsupportedOperationException()
    }

    override fun findContactWithIdentities(key: String?): Pair<Contact, MutableList<Identity>>? {
        throw UnsupportedOperationException()
    }

    override fun findWithIdentities(searchString: String?): MutableCollection<Pair<Contact, MutableList<Identity>>>? {
        throw UnsupportedOperationException()
    }

    override fun findByKeyId(keyId: String?): Contact? {
        return findByKeyId(null, keyId);
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
            else throw EntityNotFoundException("Contact not the same as the injected one")

}
