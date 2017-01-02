package de.qabel.qabelbox.chat.presenters

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
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
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class TextShareReceiverPresenterTest {

    val dropGen = DropUrlGenerator("http://example.com")
    val mainIdentity: Identity = IdentityBuilder(dropGen).withAlias("first").build()
    val identities = Identities().apply {
        put(mainIdentity)
        put(IdentityBuilder(dropGen).withAlias("second").build())
    }
    val identityInteractor: ReadOnlyIdentityInteractor = object: ReadOnlyIdentityInteractor {
        override fun getIdentity(keyId: String): Identity =
                identities.getByKeyIdentifier(keyId)

        override fun getIdentities(): Identities = identities
    }

    val contactsInteractor: ReadOnlyContactsInteractor = mock()

    val view : TextShareReceiver = mock()
    val presenter = MainTextShareReceiverPresenter(view, identityInteractor, contactsInteractor)

    @Test
    fun availableIdentities() {
        presenter.availableIdentities.map { it.alias }.toSet() eq identities
                .identities.map { it.alias }.toSet()
    }

    @Test
    fun contactsForIdentity() {
        whenever(view.identity).thenReturn(EntitySelection(mainIdentity))
        val contacts = Contacts(mainIdentity)
        contacts.put(mainIdentity.toContact())
        whenever(contactsInteractor.findContacts(mainIdentity.keyIdentifier)).thenReturn(contacts)
        presenter.contacts eq listOf(EntitySelection(mainIdentity))
    }

}

