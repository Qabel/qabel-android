package de.qabel.qabelbox.identity.interactor

import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import rx.Single

interface ReadOnlyIdentityInteractor {

    fun getIdentity(keyId: String): Identity
    fun getIdentities(): Identities
}

