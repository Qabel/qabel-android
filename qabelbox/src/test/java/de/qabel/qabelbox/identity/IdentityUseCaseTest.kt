package de.qabel.qabelbox.identity

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.extensions.CoreTestCase
import de.qabel.core.extensions.createIdentity
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.inmemory.InMemoryIdentityRepository
import de.qabel.core.repository.sqlite.SqliteIdentityRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.QblBroadcastConstants.Identities.*
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.eq
import de.qabel.qabelbox.identity.interactor.IdentityUseCase
import de.qabel.qabelbox.identity.interactor.MainIdentityUseCase
import de.qabel.qabelbox.listeners.ActionIntentSender
import de.qabel.qabelbox.persistence.RepositoryFactory
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class IdentityUseCaseTest() : CoreTestCase {

    val identity = createIdentity("Alice")

    val actionSender: ActionIntentSender = mock()
    lateinit var identityRepo: IdentityRepository
    lateinit var useCase: IdentityUseCase

    @Before
    fun setUp() {
        val factory = RepositoryFactory(RuntimeEnvironment.application)
        identityRepo = factory.getIdentityRepository()
        useCase = MainIdentityUseCase(identityRepo, actionSender)
        identityRepo.save(identity)
    }

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
        val detachedOriginal = identityRepo.find(identity.keyIdentifier, true)
        useCase.saveIdentity(identity).toBlocking().value()
        verify(actionSender).sendActionIntentBroadCast(IDENTITY_CHANGED, Pair(KEY_IDENTITY, identity), Pair(OLD_IDENTITY, detachedOriginal))
        val updated = identityRepo.find(identity.keyIdentifier)
        updated.alias eq "Forentroll"
        updated.email eq "forentroll@banane.de"
        updated.phone eq "12345678910"
    }

    @Test
    fun testDeleteIdentity() {
        useCase.deleteIdentity(identity).toBlocking().value()
        verify(actionSender).sendActionIntentBroadCast(IDENTITY_REMOVED, Pair(KEY_IDENTITY, identity))
        identityRepo.findAll().contains(identity) eq false
    }

}
