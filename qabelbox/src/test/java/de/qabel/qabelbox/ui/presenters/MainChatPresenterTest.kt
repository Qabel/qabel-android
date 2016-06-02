package de.qabel.qabelbox.ui.presenters

import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.ChatServer
import de.qabel.qabelbox.interactor.ChatUseCase
import de.qabel.qabelbox.repositories.MockContactRepository
import de.qabel.qabelbox.repositories.MockIdentityRepository
import de.qabel.qabelbox.transformers.ChatMessageTransformer
import de.qabel.qabelbox.ui.views.ChatView
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class MainChatPresenterTest {

    val identity = IdentityHelper.createIdentity("identity", null)
    val contact = IdentityHelper.createContact("contact_name")
    val chatServer = mock(ChatServer::class.java)
    val view = mock(ChatView::class.java)
    val transformer = ChatMessageTransformer(
            MockIdentityRepository(identity), MockContactRepository(contact))
    val useCase = ChatUseCase(identity, contact, transformer, chatServer)

    @Test fun testStartup() {
        val presenter = MainChatPresenter(view, useCase)
        verify(view).showEmpty()
    }
}
