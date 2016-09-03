package de.qabel.qabelbox.identity.interactor

import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.drop.DropURL
import rx.Single
import java.io.FileDescriptor

interface IdentityUseCase {

    fun getIdentity(keyId: String): Single<Identity>
    fun getIdentities(): Single<Identities>

    fun createIdentity(alias: String, dropUrl: DropURL, prefix: String,
                       email: String, phone: String): Single<Identity>

    fun saveIdentity(identity: Identity): Single<Unit>
    fun deleteIdentity(identity: Identity): Single<Unit>

    fun importIdentity(file: FileDescriptor): Single<Identity>

}
