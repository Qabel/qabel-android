package de.qabel.qabelbox.identity.interactor

import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.repository.IdentityRepository
import rx.lang.kotlin.single
import javax.inject.Inject

class MainReadOnlyIdentityInteractor @Inject constructor(
        private val identityRepository: IdentityRepository): ReadOnlyIdentityInteractor {

    override fun getIdentities() = single<Identities> { single ->
        identityRepository.findAll().let {
            single.onSuccess(it)
        }
    }

    override fun getIdentity(keyId: String) = single<Identity> {
        val identity = identityRepository.find(keyId)
        it.onSuccess(identity)
    }


}

