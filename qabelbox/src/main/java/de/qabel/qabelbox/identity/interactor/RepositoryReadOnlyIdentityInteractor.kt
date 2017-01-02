package de.qabel.qabelbox.identity.interactor

import de.qabel.core.repository.IdentityRepository
import javax.inject.Inject

class RepositoryReadOnlyIdentityInteractor @Inject constructor(
        private val identityRepository: IdentityRepository): ReadOnlyIdentityInteractor {

    override fun getIdentities() = identityRepository.findAll()

    override fun getIdentity(keyId: String) = identityRepository.find(keyId)
}

