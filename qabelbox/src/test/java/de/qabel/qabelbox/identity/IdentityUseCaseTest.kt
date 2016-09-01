package de.qabel.qabelbox.identity

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.extensions.CoreTestCase
import de.qabel.core.extensions.createIdentity
import de.qabel.core.repository.inmemory.InMemoryIdentityRepository
import de.qabel.qabelbox.QblBroadcastConstants.Identities.*
import de.qabel.qabelbox.eq
import de.qabel.qabelbox.identity.interactor.MainIdentityUseCase
import de.qabel.qabelbox.listeners.ActionIntentSender
import org.junit.Test

class IdentityUseCaseTest() : CoreTestCase {

    val identity = createIdentity("Alice")
    val identityRepo = InMemoryIdentityRepository().apply { save(identity) }

    val actionSender: ActionIntentSender = mock()
    val useCase = MainIdentityUseCase(identityRepo, actionSender)

    @Test
    fun testLoadIdentity() {
        val loaded = useCase.getIdentity(identity.keyIdentifier).toBlocking().value()
        loaded eq identity
    }

    @Test
    fun testLoadIdentities() {
        val second = createIdentity("Bob").apply { identityRepo.save(this) }
        val identities = useCase.getIdentities().toBlocking().value()
        identities.identities.size eq 2
        identities.identities eq setOf(identity, second)
    }

    @Test
    fun testUpdateIdentity() {
        identity.alias = "Forentroll"
        identity.email = "forentroll@banane.de"
        identity.phone = "12345678910"
        useCase.updateIdentity(identity).toBlocking().value()
        verify(actionSender).sendActionIntentBroadCast(IDENTITY_CHANGED, Pair(KEY_IDENTITY, identity.keyIdentifier))
        val updated = identityRepo.find(identity.keyIdentifier)
        updated eq identity
    }

    @Test
    fun testDeleteIdentity() {
        useCase.deleteIdentity(identity).toBlocking().value()
        verify(actionSender).sendActionIntentBroadCast(IDENTITY_REMOVED, Pair(KEY_IDENTITY, identity.keyIdentifier))
        identityRepo.findAll().contains(identity) eq false
    }

}
