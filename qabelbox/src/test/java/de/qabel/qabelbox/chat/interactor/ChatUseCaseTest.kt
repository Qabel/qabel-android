package de.qabel.qabelbox.chat.interactor

import com.nhaarman.mockito_kotlin.*
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
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class , constants = BuildConfig::class)
class ChatUseCaseTest {
    val identity = IdentityHelper.createIdentity("identity", null)
    val contact = IdentityHelper.createContact("contact_name")
    val transformer = ChatMessageTransformer(MockIdentityRepository(identity),
            MockContactRepository(contact, identity))
    lateinit var chatService: ChatService

    @Before
    fun setUp() {
        chatService = mock()
    }

    @Test
    fun testNoMessages() {
        val useCase = TransformingChatUseCase(identity, contact, transformer, chatService, mock())
        var list: List<ChatMessage>? = null
        useCase.retrieve().toList().toBlocking().subscribe({
            messages -> list = messages
        })
        assertThat(list, hasSize(0))
    }

    @Test
    fun testMessagesRetrieved() {
        whenever(chatService.refreshMessages()).thenReturn(
                mapOf(Pair(identity, listOf(ChatDropMessage(contact.id, identity.id,
                        ChatDropMessage.Direction.OUTGOING, ChatDropMessage.Status.NEW,
                        ChatDropMessage.MessageType.BOX_MESSAGE, "{\"msg\": \"text\"}", Date().time)))))

        val mockRepo = mock<ChatDropMessageRepository>()
        val useCase = TransformingChatUseCase(identity, contact, transformer, chatService, mockRepo)
        var list: List<ChatMessage>? = null
        useCase.retrieve().toList().toBlocking().subscribe({
            messages -> list = messages
        })
        assertThat(list, hasSize(1))
        verify(mockRepo).markAsRead(contact, identity)
    }

    @Test
    fun sendMessage() {
        val mockRepo = mock<ChatDropMessageRepository>()
        val connector = spy(MainChatService(mock(), mock(), mock(), mockRepo, mock()))
        val useCase = TransformingChatUseCase(identity, contact, transformer, chatService, mockRepo)
        var result: ChatMessage? = null
        useCase.send("Text").subscribe({
            result = it
        })
        verify(connector).sendMessage(any<ChatDropMessage>())
        verify(mockRepo).persist(any<ChatDropMessage>())
        assertThat(result, notNullValue())
    }
}
