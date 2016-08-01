package de.qabel.qabelbox.chat.interactor

import com.nhaarman.mockito_kotlin.*
import de.qabel.core.http.DropConnector
import de.qabel.core.http.MainDropConnector
import de.qabel.core.repository.ChatDropMessageRepository
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.core.service.ChatService
import de.qabel.core.service.MainChatService
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer
import de.qabel.qabelbox.repositories.MockContactRepository
import de.qabel.qabelbox.repositories.MockIdentityRepository
import de.qabel.qabelbox.tmp_core.*
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
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
    lateinit var transformer: ChatMessageTransformer
    lateinit var chatService: ChatService
    lateinit var chatDropRepository: ChatDropMessageRepository
    lateinit var dropConnector: DropConnector

    @Before
    fun setUp() {
        val identityRepo = InMemoryIdentityRepository()
        identityRepo.save(identity)
        val contactRepo = InMemoryContactRepository()
        contactRepo.save(contact, identity)
        dropConnector = MainDropConnector(MockDropServer())
        chatDropRepository = spy(InMemoryChatDropMessageRepository())
        transformer = ChatMessageTransformer(identityRepo, contactRepo)
        chatService = spy(MainChatService(dropConnector, identityRepo, contactRepo, chatDropRepository, InMemoryDropStateRepository()))
    }

    @Test
    fun testNoMessages() {
        val useCase = TransformingChatUseCase(identity, contact, transformer, chatService, chatDropRepository)
        var list: List<ChatMessage>? = null
        useCase.retrieve().toList().toBlocking().subscribe({
            messages ->
            list = messages
        })
        assertThat(list, hasSize(0))
    }

    @Test
    fun testMessagesRetrieved() {
        chatDropRepository.persist(ChatDropMessage(contact.id, identity.id,
                ChatDropMessage.Direction.OUTGOING, ChatDropMessage.Status.NEW,
                ChatDropMessage.MessageType.BOX_MESSAGE, ChatDropMessage.MessagePayload.TextMessage("test123"), Date().time))

        val useCase = TransformingChatUseCase(identity, contact, transformer, chatService, chatDropRepository)
        val list: List<ChatMessage> = useCase.retrieve().toList().toBlocking().first()
        assertThat(list, hasSize(1))
        verify(chatDropRepository).markAsRead(contact, identity)
        verify(chatDropRepository).findByContact(contact.id, identity.id)
    }

    @Test
    @Ignore("Needs fix for gson")
    fun sendMessage() {
        val useCase = TransformingChatUseCase(identity, contact, transformer, chatService, chatDropRepository)
        var result: ChatMessage? = null
        useCase.send("Text").subscribe({
            result = it
        })
        verify(chatService).sendMessage(any<ChatDropMessage>())
        verify(chatDropRepository).persist(any<ChatDropMessage>())
        assertThat(result, notNullValue())
    }
}
