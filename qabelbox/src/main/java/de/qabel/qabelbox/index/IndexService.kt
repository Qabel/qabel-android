package de.qabel.qabelbox.index

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity

//TODO coreable
interface IndexService {

    fun updateIdentity(identity: Identity)
    fun updateIdentityPhone(updatedIdentity: Identity, oldPhone: String)
    fun updateIdentityEmail(updatedIdentity: Identity, oldEmail: String)
    fun updateIdentityVerifications()

    fun deleteIdentity(identity: Identity)

    fun syncContacts(externalContacts: List<RawContact>): List<Contact>

}
