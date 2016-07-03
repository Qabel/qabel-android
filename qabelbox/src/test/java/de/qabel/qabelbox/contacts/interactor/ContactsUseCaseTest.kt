package de.qabel.qabelbox.contacts.interactor

import de.qabel.desktop.repository.ContactRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.repositories.MockContactRepository
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ContactsUseCaseTest {

    val identityA = IdentityHelper.createIdentity("identityA", null)
    val identityB = IdentityHelper.createIdentity("identityB", null)
    val contactA = IdentityHelper.createContact("contactA")
    val contactB = IdentityHelper.createContact("contactB")
    val identityContact = IdentityHelper.createContact("identityAContact")


    lateinit var contactRepo: ContactRepository;

    @Before
    fun setUp() {
        contactRepo = MockContactRepository()
    }

    @Test
    fun testLoad() {
        val contactsUseCase = MainContactsUseCase(identityA, contactRepo);
        var contacts: List<ContactDto>? = null;
        contactsUseCase.load().toList().toBlocking().subscribe {
            data ->
            contacts = data;
        }
        assertThat(contacts, hasSize(0));
    }

    @Test
    fun testLoad1() {
        contactRepo.save(contactA, identityA);
        val contactsUseCase = MainContactsUseCase(identityA, contactRepo);
        var contacts: List<ContactDto>? = null;
        contactsUseCase.load().toList().toBlocking().subscribe {
            data ->
            contacts = data;
        }
        assertThat(contacts, hasSize(1));
        val loadedContact = contacts!!.first();
        assertThat(loadedContact.contact, equalTo(contactA));
        assertThat(loadedContact.identities, hasSize(1));
        assertThat(loadedContact.identities.first(), equalTo(identityA));
    }

    @Test
    fun testLoad2() {
        contactRepo.save(contactA, identityA);
        contactRepo.save(contactA, identityB);
        contactRepo.save(contactB, identityB);
        val contactsUseCase = MainContactsUseCase(identityA, contactRepo);
        var contacts: List<ContactDto>? = null;
        contactsUseCase.load().toList().toBlocking().subscribe {
            data ->
            contacts = data;
        }
        val storedA = contacts!!.find { c -> c.contact.keyIdentifier.equals(contactA.keyIdentifier) };
        val storedB = contacts!!.find { c -> c.contact.keyIdentifier.equals(contactB.keyIdentifier) };
        assertThat(contacts, hasSize(2));

        assertThat(storedA!!.contact, equalTo(contactA));
        assertThat(storedA.identities, hasSize(2));
        assertThat(storedA.active, `is`(true))
        assertThat(storedA.identities, containsInAnyOrder(identityA, identityB))

        assertThat(storedB!!.contact, equalTo(contactB));
        assertThat(storedB.identities, hasSize(1));
        assertThat(storedB.active, `is`(false))
        assertThat(storedB.identities, containsInAnyOrder(identityB))
    }

    @Test
    fun testSearch() {
        contactRepo.save(contactA, identityA);
        contactRepo.save(contactA, identityB);
        contactRepo.save(identityContact, identityB);

        val contactsUseCase = MainContactsUseCase(identityA, contactRepo);

        var contacts: List<ContactDto>? = null;
        contactsUseCase.search("identity").toList().toBlocking().subscribe {
            data -> contacts = data;
        }
        assertThat(contacts, hasSize(1));
        val foundContact = contacts!!.first();
        assertThat(foundContact.contact, equalTo(identityContact));
        assertThat(foundContact.identities, hasSize(1));
        assertThat(foundContact.identities.first(), equalTo(identityB));
    }

    @Test
    fun testLoadContact() {
        contactRepo.save(contactA, identityA);
        contactRepo.save(contactA, identityB);

        val contactsUseCase = MainContactsUseCase(identityA, contactRepo);

        var contact: ContactDto? = null;
        contactsUseCase.loadContact(contactA.keyIdentifier).toBlocking().subscribe {
            data -> contact = data;
        }
        assertThat(contact, notNullValue());
        assertThat(contact!!.contact, equalTo(contactA))
        assertThat(contact!!.identities, hasSize(2))
        assertThat(contact!!.identities, containsInAnyOrder(identityA, identityB))
    }

}
