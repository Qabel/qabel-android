package de.qabel.qabelbox.interactor

import com.nhaarman.mockito_kotlin.verify
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.ChatServer
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.repositories.MockContactRepository
import de.qabel.qabelbox.repositories.MockIdentityRepository
import de.qabel.qabelbox.transformers.ChatMessageTransformer
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config

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
        chatServer = mock(ChatServer::class.java)
    }

    @Test
    fun testNoMessages() {
        val useCase = TransformingChatUseCase(identity, contact, transformer, chatServer)
        var list: List<ChatMessage>? = null
        useCase.retrieve().subscribe({
            messages -> list = messages
        })
        assertThat(list, hasSize(0))
        verify(chatServer).setAllMessagesRead(identity, contact)
    }
}
