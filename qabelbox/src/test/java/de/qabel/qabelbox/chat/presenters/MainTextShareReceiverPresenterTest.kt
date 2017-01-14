package de.qabel.qabelbox.chat.presenters

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.chat.service.ChatService
import de.qabel.core.config.Contact
import de.qabel.core.config.Contacts
import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.config.factory.DropUrlGenerator
import de.qabel.core.config.factory.IdentityBuilder
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.view.presenters.MainTextShareReceiverPresenter
import de.qabel.qabelbox.chat.view.views.TextShareReceiver
import de.qabel.qabelbox.contacts.dto.EntitySelection
import de.qabel.qabelbox.contacts.interactor.ReadOnlyContactsInteractor
import de.qabel.qabelbox.eq
import de.qabel.qabelbox.identity.interactor.ReadOnlyIdentityInteractor
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import rx.Observable
import rx.lang.kotlin.toSingletonObservable
import java.io.IOError


@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class MainTextShareReceiverPresenterTest {

    val dropGen = DropUrlGenerator("http://example.com")
    val mainIdentity: Identity = IdentityBuilder(dropGen).withAlias("first").build()

    val secondIdentity: Identity = IdentityBuilder(dropGen).withAlias("second").build()
    val contact: Contact = secondIdentity.toContact()
    val chatDropMessage = ChatDropMessage(1, 1,
            ChatDropMessage.Direction.OUTGOING, ChatDropMessage.Status.SENT,
            ChatDropMessage.MessageType.BOX_MESSAGE, """{ "msg": "foo"}""", 1L, 0)

    val identities = Identities().apply {
        put(mainIdentity)
        put(secondIdentity)
    }
    val identityInteractor: ReadOnlyIdentityInteractor = object: ReadOnlyIdentityInteractor {
        override fun getIdentity(keyId: String): Identity =
                identities.getByKeyIdentifier(keyId)

        override fun getIdentities(): Identities = identities
    }

    val contactsInteractor: ReadOnlyContactsInteractor = object: ReadOnlyContactsInteractor {
        override fun findContacts(identityKeyId: String) =
                Contacts(mainIdentity).apply { put(contact) }

    }

    open class MockView(override var identity: EntitySelection?,
                        override var contact: EntitySelection?) : TextShareReceiver {
        override var text: String = ""

        override fun stop() { }

        override fun showError() { }
    }

    val view : MockView = spy(MockView(EntitySelection(mainIdentity), EntitySelection(contact)))

    val chatService: ChatService = mock()

    val presenter = MainTextShareReceiverPresenter(
            view, identityInteractor, contactsInteractor, chatService)

    @Test
    fun availableIdentities() {
        presenter.availableIdentities.map { it.alias }.toSet() eq identities
                .identities.map { it.alias }.toSet()
    }

    @Test
    fun contactsForIdentity() {
        presenter.contacts eq listOf(EntitySelection(contact))
    }

    @Test
    fun confirmOnlyWhenContactAndIdentityAreChosen() {
        view.identity = null
        view.contact = null
        presenter.confirm()
        verify(view, never()).stop()
    }

    @Test
    fun confirmSendsMessage() {
        val msg = "foo"
        view.text = msg
        whenever(chatService.sendTextMessage(msg, mainIdentity, contact)).thenReturn(
                chatDropMessage.toSingletonObservable())
        presenter.confirm()
        verify(view).stop()
        verify(chatService).sendTextMessage(msg, mainIdentity, contact)
    }

    @Test
    fun showsErrorWithoutIdentity() {
        view.identity = null
        presenter.confirm()
        verify(view).showError()
    }

    @Test
    fun showsErrorWithoutContact() {
        view.contact = null
        presenter.confirm()
        verify(view).showError()
    }

    @Test
    fun showsErrorWithInvalidContact() {
        view.contact = EntitySelection(mainIdentity.toContact())
        presenter.confirm()
        verify(view).showError()
    }

    @Test
    fun showsErrorWhenSendingFails() {
        val result: Observable<ChatDropMessage> = Observable.fromCallable {
            throw Exception("stuff happened")
        }
        whenever(chatService.sendTextMessage("", mainIdentity, contact)).thenReturn(result)
        presenter.confirm()
        verify(view).showError()
    }


}

