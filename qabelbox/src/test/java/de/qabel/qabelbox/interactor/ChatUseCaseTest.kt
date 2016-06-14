package de.qabel.qabelbox.interactor

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.*
import de.qabel.core.drop.DropMessage
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.ChatMessageItem
import de.qabel.qabelbox.chat.ChatServer
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.repositories.MockContactRepository
import de.qabel.qabelbox.repositories.MockIdentityRepository
import de.qabel.qabelbox.services.MockedDropConnector
import de.qabel.qabelbox.transformers.ChatMessageTransformer
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
            MockContactRepository(contact))
    lateinit var chatServer: ChatServer

    @Before
    fun setUp() {
        chatServer = mock()
    }

    @Test
    fun testNoMessages() {
        val useCase = TransformingChatUseCase(identity, contact, transformer, chatServer, mock())
        var list: List<ChatMessage>? = null
        useCase.retrieve().toList().toBlocking().subscribe({
            messages -> list = messages
        })
        assertThat(list, hasSize(0))
    }

    @Test
    fun testMessagesRetrieved() {
        whenever(chatServer.getAllMessages(identity, contact)).thenReturn(
                arrayOf(ChatMessageItem(1, 1, Date().time, contact.keyIdentifier, identity.keyIdentifier,
                        "1", ChatMessageItem.BOX_MESSAGE, "{\"msg\": \"text\"}")))
        val useCase = TransformingChatUseCase(identity, contact, transformer, chatServer, mock())
        var list: List<ChatMessage>? = null
        useCase.retrieve().toList().toBlocking().subscribe({
            messages -> list = messages
        })
        assertThat(list, hasSize(1))
        verify(chatServer).setAllMessagesRead(identity, contact)
    }

    @Test
    fun sendMessage() {
        val connector = spy(MockedDropConnector())
        val useCase = TransformingChatUseCase(identity, contact, transformer, chatServer, connector)
        var result: ChatMessage? = null
        useCase.send("Text").subscribe({
            result = it
        })
        verify(connector).sendDropMessage(any<DropMessage>(), eq(contact), eq(identity), any())
        verify(chatServer).storeIntoDB(eq(identity), any())
        connector.messages.size shouldMatch equalTo(1)
        assertThat(result, notNullValue())
    }
}
