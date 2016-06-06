package de.qabel.qabelbox.ui.presenters

import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.dto.MessagePayload
import de.qabel.qabelbox.interactor.MockChatUseCase
import de.qabel.qabelbox.ui.views.ChatView
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class MainChatPresenterTest {

    val identity = IdentityHelper.createIdentity("identity", null)
    val contact = IdentityHelper.createContact("contact_name")
    val view = mock(ChatView::class.java)
    val useCase = MockChatUseCase(contact, listOf())
    val now = Date()
    val textPayload = MessagePayload.TextMessage("Text")
    val sampleMessage = ChatMessage(identity, contact, ChatMessage.Direction.INCOMING, now, textPayload)

    @Test fun testEmptyStartup() {
        MainChatPresenter(view, useCase)
        verify(view).showEmpty()
    }

    @Test fun testStartupWithMessages() {
        useCase.messages = listOf(sampleMessage,
                sampleMessage.copy(messagePayload = MessagePayload.TextMessage("test2")))
        MainChatPresenter(view, useCase)
        verify(view).showMessages(useCase.messages)
    }
}
