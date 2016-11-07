package de.qabel.qabelbox.identity.interactor

import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.config.IdentityExportImport
import de.qabel.core.config.Prefix
import de.qabel.core.crypto.QblECKeyPair
import de.qabel.core.drop.DropURL
import de.qabel.core.extensions.letApply
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.exception.EntityExistsException
import de.qabel.qabelbox.QblBroadcastConstants.Identities.*
import de.qabel.qabelbox.listeners.ActionIntentSender
import rx.Single
import rx.lang.kotlin.single
import java.io.FileDescriptor
import java.io.FileInputStream
import javax.inject.Inject


class MainIdentityInteractor @Inject constructor(private val identityRepository: IdentityRepository,
                                                 private val actionEventSender: ActionIntentSender)
: IdentityInteractor, ReadOnlyIdentityInteractor by MainReadOnlyIdentityInteractor(identityRepository) {

    override fun saveIdentity(identity: Identity): Single<Identity> =
            if (identity.id == 0)
                createIdentity({ identity })
            else
                updateIdentity(identity)

    private fun updateIdentity(identity: Identity) = single<Identity> {
        val oldIdentity = identityRepository.find(identity.keyIdentifier, true)
        identityRepository.save(identity)
        actionEventSender.sendActionIntentBroadCast(IDENTITY_CHANGED, Pair(KEY_IDENTITY, identity), Pair(OLD_IDENTITY, oldIdentity))
        it.onSuccess(identity)
    }

    private fun createIdentity(createIdentity: () -> Identity) = single<Identity> {
        val identity = createIdentity()
        identityRepository.save(identity)
        actionEventSender.sendActionIntentBroadCast(IDENTITY_CREATED, Pair(KEY_IDENTITY, identity))
        it.onSuccess(identity)
    }

    override fun deleteIdentity(identity: Identity) = single<Unit> {
        identityRepository.delete(identity)
        actionEventSender.sendActionIntentBroadCast(IDENTITY_REMOVED, Pair(KEY_IDENTITY, identity))
        it.onSuccess(Unit)
    }

    override fun createIdentity(alias: String, dropUrl: DropURL, prefix: String, email: String, phone: String) = single<Identity> {
        val identity = Identity(alias, mutableListOf(dropUrl), QblECKeyPair()).letApply {
            it.prefixes = mutableListOf(Prefix(prefix))
            it.email = email
            it.phone = phone
        }
        identityRepository.save(identity)
        actionEventSender.sendActionIntentBroadCast(IDENTITY_CREATED, Pair(KEY_IDENTITY, identity))
        it.onSuccess(identity)
    }

    override fun importIdentity(file: FileDescriptor) = createIdentity {
        val inputString = FileInputStream(file).use { stream ->
            stream.reader().readText()
        }
        val importedIdentity = IdentityExportImport.parseIdentity(inputString)
        if (identityRepository.findAll().contains(importedIdentity.keyIdentifier))
            throw EntityExistsException("Identity already exists")

        importedIdentity
    }

}
