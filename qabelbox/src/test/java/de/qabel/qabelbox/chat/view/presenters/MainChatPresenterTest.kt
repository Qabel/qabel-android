package de.qabel.qabelbox.chat.view.presenters

import com.nhaarman.mockito_kotlin.*
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.chat.interactor.MockChatUseCase
import de.qabel.qabelbox.chat.view.views.ChatView
import de.qabel.qabelbox.navigation.Navigator
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import rx.lang.kotlin.toSingletonObservable
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class MainChatPresenterTest {

    val identity = IdentityHelper.createIdentity("identity", null)
    val contact = IdentityHelper.createContact("contact_name")
    val view: ChatView = mock(ChatView::class.java)
    val now = Date()
    val textPayload = MessagePayloadDto.TextMessage("Text")
    val sampleMessage = ChatMessage(identity, contact, ChatDropMessage.Direction.INCOMING, now, textPayload)
    val useCase: MockChatUseCase = spy(MockChatUseCase(sampleMessage, contact, listOf()))

    val navigator : Navigator = mock(Navigator::class.java)
    val presenter = MainChatPresenter(view, useCase, navigator)

    @Test fun testEmptyStartup() {
        presenter.refreshMessages()
        verify(view).reset()
    }

    @Test fun testStartupWithMessages() {
        useCase.messages = listOf(sampleMessage,
                sampleMessage.copy(messagePayload = MessagePayloadDto.TextMessage("test2")))
        presenter.refreshMessages()
        verify(view).prependData(useCase.messages)
    }

    @Test fun messageIsSent() {
        stub(view.messageText).toReturn("Text")
        presenter.sendMessage()
        verify(useCase).send(view.messageText)
        verify(view).appendData(listOf(sampleMessage))
        verify(view).messageText = ""
    }

    @Test fun emptyMessageIsIgnored() {
        stub(view.messageText).toReturn("")
        presenter.sendMessage()
        verify(useCase, never()).send(any())
        verify(view, never()).prependData(any())
    }

    @Test
    fun testHandleAddContactClick(){
        presenter.handleContactAddClick()
        verify(useCase).addContact()
        verify(view).refreshContactOverlay()
    }

    @Test
    fun testHandleIgnoreContactClick(){
        whenever(useCase.contact).thenReturn(contact)
        presenter.handleContactIgnoreClick()
        verify(useCase).ignoreContact()
        verify(navigator).popBackStack()
    }

}
