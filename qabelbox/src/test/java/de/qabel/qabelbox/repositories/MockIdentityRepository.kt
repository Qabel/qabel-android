package de.qabel.qabelbox.repositories

import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.exception.EntityNotFoundException

class MockIdentityRepository(val identity: Identity): IdentityRepository {

    private val identityId = 1

    override fun find(keyId: String?): Identity? = findOrError(keyId == identity.keyIdentifier)

    override fun find(id: Int): Identity? = findOrError(id == identityId)

    override fun findAll(): Identities? = Identities().apply { put(identity) }

    override fun save(identity: Identity?) = throw UnsupportedOperationException()

    override fun delete(identity: Identity?) = throw UnsupportedOperationException()

    private fun findOrError(boolean: Boolean): Identity {
        if (boolean) { return identity }
        throw EntityNotFoundException("Identity not the same as the injected one")
    }

}
