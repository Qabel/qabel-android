package de.qabel.qabelbox.identity.interactor

import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import rx.Single

interface IdentityUseCase {

    fun getIdentity(keyId: String): Single<Identity>
    fun getIdentities(): Single<Identities>

    fun updateIdentity(identity: Identity): Single<Unit>
    fun deleteIdentity(identity: Identity): Single<Unit>

}
