package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.repository.ChatDropMessageRepository
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer
import de.qabel.qabelbox.tmp_core.InMemoryChatDropMessageRepository
import de.qabel.qabelbox.tmp_core.InMemoryContactRepository
import de.qabel.qabelbox.tmp_core.InMemoryIdentityRepository
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.util.*

import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ChatServiceUseCaseTest {

    val identityA = IdentityHelper.createIdentity("identityA", null).apply { id = 1 }
    val contactA = IdentityHelper.createContact("contactA").apply { id = 2 }
    val identityB = IdentityHelper.createIdentity("identityB", null).apply { id = 3 }
    val contactB = IdentityHelper.createContact("contactB").apply { id = 4 }

    lateinit var contactRepo: ContactRepository
    lateinit var messageRepo: ChatDropMessageRepository
    lateinit var contactsUseCase: ChatServiceUseCase

    lateinit var message: ChatDropMessage

    @Before
    fun setUp() {
        val identityRepo = InMemoryIdentityRepository()
        identityRepo.save(identityA)
        identityRepo.save(identityB)
        contactRepo = InMemoryContactRepository()
        messageRepo = InMemoryChatDropMessageRepository()
        contactsUseCase = MainChatServiceUseCase(messageRepo, contactRepo, identityRepo, ChatMessageTransformer(identityRepo, contactRepo))
    }

    private fun createMsg(contact: Contact, identity: Identity, status: ChatDropMessage.Status) =
            ChatDropMessage(contact.id, identity.id,
                    ChatDropMessage.Direction.OUTGOING, status,
                    ChatDropMessage.MessageType.BOX_MESSAGE, ChatDropMessage.MessagePayload.TextMessage("test"), Date().time)

    @Test
    fun testMarkIdentityMessagesRead() {
        contactRepo.save(contactA, identityB)
        contactRepo.save(contactB, identityB)
        messageRepo.persist(createMsg(contactA, identityB, ChatDropMessage.Status.NEW))
        messageRepo.persist(createMsg(contactB, identityB, ChatDropMessage.Status.NEW))
        messageRepo.persist(createMsg(contactB, identityA, ChatDropMessage.Status.NEW))
        contactsUseCase.markIdentityMessagesRead(identityB.keyIdentifier)
        assertThat(messageRepo.findNew(identityA.id), hasSize(1))
        assertThat(messageRepo.findNew(identityB.id), hasSize(0))
    }

    @Test
    fun testMarkContactMessagesRead() {
        contactRepo.save(contactA, identityB)
        contactRepo.save(contactB, identityB)
        messageRepo.persist(createMsg(contactA, identityB, ChatDropMessage.Status.NEW))
        messageRepo.persist(createMsg(contactB, identityB, ChatDropMessage.Status.NEW))
        messageRepo.persist(createMsg(contactB, identityB, ChatDropMessage.Status.NEW))
        contactsUseCase.markContactMessagesRead(identityB.keyIdentifier, contactA.keyIdentifier)
        val result = messageRepo.findNew(identityB.id)
        assertThat(result, hasSize(2))
    }

    @Test
    fun testAddContact() {
        contactA.status = Contact.ContactStatus.UNKNOWN
        contactRepo.save(contactA, identityA)
        contactsUseCase.addContact(identityA.keyIdentifier, contactA.keyIdentifier)
        assertThat(contactA.status, equalTo(Contact.ContactStatus.NORMAL))
        assertThat(contactRepo.find(contactA.id).status, equalTo(Contact.ContactStatus.NORMAL))
    }

    @Test
    fun testIgnoreContact() {
        contactRepo.save(contactA, identityA)
        contactsUseCase.ignoreContact(identityA.keyIdentifier, contactA.keyIdentifier)
        assertTrue(contactA.isIgnored)
        assertTrue(contactRepo.find(contactA.id).isIgnored)
    }

    @Test
    fun testGetNewMessageAffectedKeyIds() {
        contactRepo.save(contactA, identityB)
        contactRepo.save(contactB, identityB)
        contactRepo.save(contactB, identityA)
        messageRepo.persist(createMsg(contactA, identityB, ChatDropMessage.Status.NEW))
        messageRepo.persist(createMsg(contactB, identityB, ChatDropMessage.Status.NEW))
        messageRepo.persist(createMsg(contactB, identityA, ChatDropMessage.Status.NEW))
        val result = contactsUseCase.getNewMessageAffectedKeyIds()
        assertThat(result, containsInAnyOrder(contactA.keyIdentifier,
                contactB.keyIdentifier,
                identityA.keyIdentifier,
                identityB.keyIdentifier))
    }

    @Test
    fun testGetNewMessageMap() {
        val messages = listOf(createMsg(contactA, identityB, ChatDropMessage.Status.NEW),
                createMsg(contactB, identityB, ChatDropMessage.Status.NEW),
                createMsg(contactB, identityA, ChatDropMessage.Status.NEW))
        messages.forEach { messageRepo.persist(it) }
        contactRepo.save(contactA, identityB)
        contactRepo.save(contactB, identityB)
        contactRepo.save(contactB, identityA)

        val result = contactsUseCase.getNewMessageMap()
        assertThat(result.keys, hasSize(2))
        assertThat(result[identityB], hasSize(2))
        assertThat(result[identityA], hasSize(1))
    }

}
