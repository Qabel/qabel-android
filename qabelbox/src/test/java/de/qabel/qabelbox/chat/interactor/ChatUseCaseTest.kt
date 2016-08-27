package de.qabel.qabelbox.chat.interactor

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.config.Contact
import de.qabel.core.http.DropConnector
import de.qabel.core.http.MainDropConnector
import de.qabel.core.http.MockDropServer
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.core.repository.framework.PagingResult
import de.qabel.chat.repository.inmemory.InMemoryChatDropMessageRepository
import de.qabel.core.repository.inmemory.InMemoryContactRepository
import de.qabel.core.repository.inmemory.InMemoryDropStateRepository
import de.qabel.core.repository.inmemory.InMemoryIdentityRepository
import de.qabel.chat.service.ChatService
import de.qabel.chat.service.MainChatService
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ChatUseCaseTest {

    val identity = IdentityHelper.createIdentity("identity", null)
    val contact = IdentityHelper.createContact("contact_name")

    val dropServer = MockDropServer()
    val chatServiceUseCase: ChatServiceUseCase = mock()
    var dropConnector: DropConnector = MainDropConnector(dropServer)

    lateinit var transformer: ChatMessageTransformer
    lateinit var chatService: ChatService
    lateinit var chatDropRepository: InMemoryChatDropMessageRepository

    lateinit var chatUseCase: ChatUseCase

    @Before
    fun setUp() {
        val identityRepo = InMemoryIdentityRepository()
        identityRepo.save(identity)
        val contactRepo = InMemoryContactRepository()
        contactRepo.save(contact, identity)
        chatDropRepository = spy(InMemoryChatDropMessageRepository())
        transformer = ChatMessageTransformer(identityRepo, contactRepo)
        chatService = spy(MainChatService(dropConnector, identityRepo, contactRepo, chatDropRepository, InMemoryDropStateRepository()))
        chatUseCase = TransformingChatUseCase(identity, contact, transformer, chatService, chatDropRepository, chatServiceUseCase)
    }

    @Test
    fun testNoMessages() {
        val result: PagingResult<ChatMessage> = chatUseCase.load(0, 50).toBlocking().first()
        assertThat(result.availableRange, equalTo(0))
        assertThat(result.result, hasSize(0))
    }

    @Test
    fun testMessagesRetrieved() {
        chatDropRepository.persist(ChatDropMessage(contact.id, identity.id,
                ChatDropMessage.Direction.OUTGOING, ChatDropMessage.Status.NEW,
                ChatDropMessage.MessageType.BOX_MESSAGE, ChatDropMessage.MessagePayload.TextMessage("test123"), Date().time))

        val result: PagingResult<ChatMessage> = chatUseCase.load(0, 50).toBlocking().first()
        assertThat(result.availableRange, equalTo(1))
        assertThat(result.result, hasSize(1))
        verify(chatDropRepository).markAsRead(contact, identity)
        verify(chatDropRepository).findByContact(contact.id, identity.id)
    }

    @Test
    fun sendMessage() {
        val result: ChatMessage = chatUseCase.send("Text").toBlocking().single()

        assertThat(chatDropRepository.findByContact(contact.id, identity.id), hasSize(1))
        assertThat(dropServer.receiveMessageBytes(contact.dropUrls.first().uri, "").third, hasSize(1))

        assertThat(result.identity, equalTo(identity))
        assertThat(result.contact, equalTo(contact))
        assertThat(result.direction, equalTo(ChatDropMessage.Direction.OUTGOING))
        assertThat(result.messagePayload.toMessage(), equalTo("Text"))
    }

    @Test
    fun testIgnoreContact() {
        chatUseCase.ignoreContact().toBlocking().subscribe()
        verify(chatServiceUseCase).ignoreContact(identity.keyIdentifier, contact.keyIdentifier)
        assertThat(contact.isIgnored, equalTo(true))
        assertThat(contact.status, equalTo(Contact.ContactStatus.NORMAL))
    }

    @Test
    fun testAddContact() {
        chatUseCase.addContact().toBlocking().subscribe()
        verify(chatServiceUseCase).addContact(identity.keyIdentifier, contact.keyIdentifier)
        assertThat(contact.isIgnored, equalTo(false))
        assertThat(contact.status, equalTo(Contact.ContactStatus.NORMAL))
    }
}
