package de.qabel.desktop.repository

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.desktop.repository.ContactRepository
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.test.TestConstants
import de.qabel.qabelbox.util.BoxTestHelper
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ContactRepositoryTest {

    lateinit var contactRepository: ContactRepository
    lateinit var identityRepository: IdentityRepository

    val storedContacts = ArrayList<ContactDto>(5);

    @Before
    fun setUp() {
        val boxTestHelper = BoxTestHelper(RuntimeEnvironment.application as QabelBoxApplication?);
        contactRepository = boxTestHelper.contactRepository;
        identityRepository = boxTestHelper.identityRepository;

        val identityA = IdentityHelper.createIdentity("Identity A", TestConstants.PREFIX);
        val identityB = IdentityHelper.createIdentity("Identity B", TestConstants.PREFIX);
        storedContacts.add(ContactDto(Contact(identityA.alias, identityA.dropUrls, identityA.ecPublicKey), emptyList()));
        storedContacts.add(ContactDto(Contact(identityB.alias, identityB.dropUrls, identityB.ecPublicKey), emptyList()));

        identityRepository.save(identityA);
        identityRepository.save(identityB);

        var i = 0;
        do {
            var contact = IdentityHelper.createContact("TestContact" + i);
            val contactIdentities = LinkedList<Identity>();
            contactIdentities.add(identityA);
            contactRepository.save(contact, identityA);

            if (i % 2 == 0) {
                contactIdentities.add(identityB)
                contactRepository.save(contact, identityB);
            };
            storedContacts.add(ContactDto(contact, contactIdentities));
            i++;
        } while (i < 5)
        var contact = IdentityHelper.createContact("XFoo");
        contactRepository.save(contact, identityB);
        storedContacts.add(ContactDto(contact, Arrays.asList(identityB)));
    }


    @Test
    fun testFindAll() {
        val contacts = contactRepository.findWithIdentities();
        var index = 0;
        for (contact in contacts) {
            val storedDto = storedContacts[index];
            assertThat(storedDto.contact.alias, equalTo(contact.first.alias));
            assertThat(storedDto.identities, hasSize(contact.second.size));
            index++;
        }
        assertThat(storedContacts, hasSize(contacts.size));
    }

    @Test
    fun testFindAllFiltered() {
        val filter = "xfo";
        val contacts = contactRepository.findWithIdentities(filter);
        Assert.assertEquals(1, contacts.size)
        var fooContact = contacts.iterator().next();
        assertThat("XFoo", equalTo(fooContact.first.alias));
        assertThat(1, equalTo(fooContact.second.size));
    }

}
