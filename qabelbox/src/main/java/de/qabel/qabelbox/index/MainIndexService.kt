package de.qabel.qabelbox.index

import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.index.IndexServer
import de.qabel.core.index.UpdateAction
import de.qabel.core.index.UpdateIdentity
import de.qabel.core.repository.ContactRepository

class MainIndexService(private val indexServer: IndexServer,
                       private val contactRepository: ContactRepository) : IndexService {

    override fun updateIdentity(identity: Identity) {
        UpdateIdentity.fromIdentity(identity, UpdateAction.CREATE).let {
            indexServer.updateIdentity(it)
        }
    }

    override fun deleteIdentity(identity: Identity) {
        UpdateIdentity.fromIdentity(identity, UpdateAction.DELETE).let {
            indexServer.updateIdentity(it)
        }
    }

    override fun syncContacts(identities: Identities, contacts: List<RawContact>) {
        TODO()
    }

}
