package de.qabel.qabelbox.index

import de.qabel.core.config.Identities
import de.qabel.core.config.Identity


interface IndexService {

    open fun updateIdentity(identity: Identity)
    open fun deleteIdentity(identity: Identity)
    open fun syncContacts(identities: Identities, contacts: List<RawContact>)
}
