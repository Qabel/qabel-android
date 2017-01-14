package de.qabel.qabelbox.contacts.interactor

import de.qabel.core.config.Contacts
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import javax.inject.Inject

class RepositoryReadOnlyContactsInteractor @Inject constructor(
        val contactRepository: ContactRepository,
        val identityRepository: IdentityRepository
): ReadOnlyContactsInteractor {

    override fun findContacts(identityKeyId: String): Contacts =
        contactRepository.find(identityRepository.find(identityKeyId))

}

