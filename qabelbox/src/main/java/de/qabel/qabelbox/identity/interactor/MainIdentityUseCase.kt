package de.qabel.qabelbox.identity.interactor

import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.config.IdentityExportImport
import de.qabel.core.crypto.QblECKeyPair
import de.qabel.core.drop.DropURL
import de.qabel.core.extensions.letApply
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.exception.EntityExistsException
import de.qabel.core.repository.sqlite.SqliteIdentityRepository
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.QblBroadcastConstants.Identities.*
import de.qabel.qabelbox.contacts.dto.ContactsParseResult
import de.qabel.qabelbox.listeners.ActionIntentSender
import rx.Single
import rx.lang.kotlin.*
import java.io.FileDescriptor
import java.io.FileInputStream
import javax.inject.Inject


class MainIdentityUseCase @Inject constructor(private val identityRepository: IdentityRepository,
                                              private val actionEventSender: ActionIntentSender) : IdentityUseCase {

    override fun getIdentities() = single<Identities> { single ->
        identityRepository.findAll().let {
            single.onSuccess(it)
        }
    }

    override fun getIdentity(keyId: String) = single<Identity> {
        val identity = identityRepository.find(keyId)
        it.onSuccess(identity)
    }

    override fun saveIdentity(identity: Identity) = single<Unit> {
        identityRepository.save(identity)
        actionEventSender.sendActionIntentBroadCast(IDENTITY_CHANGED, Pair(KEY_IDENTITY, identity))
        it.onSuccess(Unit)
    }

    override fun deleteIdentity(identity: Identity) = single<Unit> {
        identityRepository.delete(identity)
        actionEventSender.sendActionIntentBroadCast(IDENTITY_REMOVED, Pair(KEY_IDENTITY, identity))
        it.onSuccess(Unit)
    }

    override fun createIdentity(alias: String, dropUrl: DropURL, prefix: String, email: String, phone: String) = single<Identity> {
        val identity = Identity(alias, mutableListOf(dropUrl), QblECKeyPair()).letApply {
            it.prefixes = mutableListOf(prefix)
            it.email = email
            it.phone = phone
        }
        identityRepository.save(identity)
        it.onSuccess(identity)
    }

    override fun importIdentity(file: FileDescriptor) = single<Identity> {
        val inputString = FileInputStream(file).use { stream ->
            stream.reader().readText()
        }
        val importedIdentity = IdentityExportImport.parseIdentity(inputString)
        if (identityRepository.findAll().contains(importedIdentity.keyIdentifier))
            throw EntityExistsException("Identity already exists")

        identityRepository.save(importedIdentity)
        it.onSuccess(importedIdentity)
    }

}
