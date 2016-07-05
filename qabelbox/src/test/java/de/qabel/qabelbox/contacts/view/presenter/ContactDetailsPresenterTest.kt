package de.qabel.qabelbox.contacts.view.presenter

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import de.qabel.desktop.repository.ContactRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase
import de.qabel.qabelbox.contacts.interactor.MainContactsUseCase
import de.qabel.qabelbox.contacts.view.presenters.ContactDetailsPresenter
import de.qabel.qabelbox.contacts.view.presenters.MainContactDetailsPresenter
import de.qabel.qabelbox.contacts.view.views.ContactDetailsView
import de.qabel.qabelbox.repositories.MockContactRepository
import de.qabel.qabelbox.test.TestConstants
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ContactDetailsPresenterTest {

    val identity = IdentityHelper.createIdentity("Identity", TestConstants.PREFIX);
    val contactA = IdentityHelper.createContact("ContactA");
    val contactADto = ContactDto(contactA, listOf(identity));

    var contactRepo: ContactRepository = MockContactRepository();

    init {
        contactRepo.save(contactA, identity);
    }

    lateinit var contactUseCase: ContactsUseCase
    lateinit var detailsView: ContactDetailsView
    lateinit var presenter: ContactDetailsPresenter;

    @Before
    fun setUp() {
        contactUseCase = spy(MainContactsUseCase(identity, contactRepo));
        detailsView = mock();
        Mockito.`when`(detailsView.contactKeyId).thenAnswer { contactA.keyIdentifier }
        presenter = MainContactDetailsPresenter(detailsView, contactUseCase);
    }

    @Test
    fun testRefresh() {
        presenter.refreshContact();
        verify(contactUseCase).loadContact(contactA.keyIdentifier);
        assertThat(presenter.title, equalTo(contactA.alias));
        verify(detailsView).loadContact(contactADto);
    }

}
