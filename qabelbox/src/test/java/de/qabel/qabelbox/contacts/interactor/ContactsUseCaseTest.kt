package de.qabel.qabelbox.contacts.interactor

import de.qabel.core.contacts.ContactExchangeFormats
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.config.QabelSchema
import de.qabel.qabelbox.contacts.ContactMatcher
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.repositories.MockContactRepository
import de.qabel.qabelbox.test.files.FileHelper
import de.qabel.qabelbox.util.IdentityHelper
import org.apache.commons.io.FileUtils
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

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
            data ->
            contacts = data;
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
            data ->
            contact = data;
        }
        assertThat(contact, notNullValue());
        assertThat(contact!!.active, `is`(true))
        assertThat(contact!!.contact, equalTo(contactA))
        assertThat(contact!!.identities, hasSize(2))
        assertThat(contact!!.identities, containsInAnyOrder(identityA, identityB))
    }

    @Test
    fun testSaveContact() {
        contactRepo.save(contactA, identityA);
        val contactsUseCase = MainContactsUseCase(identityA, contactRepo);
        contactsUseCase.saveContact(contactB).toBlocking().subscribe {
        }

        val resultContact = contactRepo.findByKeyId(identityA, contactB.keyIdentifier);
        assertThat(resultContact, equalTo(contactB));
    }

    @Test
    fun testDeleteContact() {
        contactRepo.save(contactA, identityA);
        contactRepo.save(contactB, identityA);
        val contactsUseCase = MainContactsUseCase(identityA, contactRepo);
        contactsUseCase.deleteContact(contactB).toBlocking().subscribe {
        }
        assertThat(contactRepo.exists(contactB), `is`(false));
    }

    @Test
    fun testExportContactToDirectory() {
        contactRepo.save(contactA, identityA);
        val contactsUseCase = MainContactsUseCase(identityA, contactRepo);
        val contactExchangeFormats = ContactExchangeFormats();
        val tmpFolder = FileHelper.createTmpDir();

        var exportedContactFile: File? = null;
        contactsUseCase.exportContact(contactA.keyIdentifier, tmpFolder).toBlocking().subscribe {
            resultFile ->
            exportedContactFile = resultFile;
        };

        assertThat(exportedContactFile, notNullValue());
        assertThat(exportedContactFile!!.exists(), `is`(true));
        assertThat(listOf(*tmpFolder.listFiles()), containsInAnyOrder(exportedContactFile));
        assertThat(exportedContactFile!!.name, equalTo(QabelSchema.createContactFilename(contactA.alias)))

        val importedContacts = contactExchangeFormats.
                importFromContactsJSON(FileUtils.readFileToString(exportedContactFile));
        assertThat(importedContacts, hasSize(1))
        assertThat(contactA, ContactMatcher(importedContacts.first()));
    }

    @Test
    fun testExportContactToFile() {
        contactRepo.save(contactA, identityA);
        contactRepo.save(contactB, identityA);
        val contactsUseCase = MainContactsUseCase(identityA, contactRepo);
        val contactExchangeFormats = ContactExchangeFormats();
        val tmpFile = FileHelper.createEmptyTargetFile();

        var exportedCount: Int? = null;
        FileOutputStream(tmpFile).use { outStream ->
            contactsUseCase.exportContact(contactB.keyIdentifier, outStream.fd).toBlocking().subscribe {
                count ->
                exportedCount = count;
            };
        }
        assertThat(exportedCount, `is`(1));

        val importedContacts = contactExchangeFormats.
                importFromContactsJSON(FileUtils.readFileToString(tmpFile));
        assertThat(importedContacts, hasSize(1))
        assertThat(importedContacts.first(), ContactMatcher(contactB));
    }

    @Test
    fun testExportAllContactsToFile() {
        contactRepo.save(contactA, identityA);
        contactRepo.save(contactB, identityA);
        contactRepo.save(identityContact, identityA);
        val contactsUseCase = MainContactsUseCase(identityA, contactRepo);
        val contactExchangeFormats = ContactExchangeFormats();
        val tmpFile = FileHelper.createEmptyTargetFile();

        var exportedCount: Int? = null;
        FileOutputStream(tmpFile).use { outStream ->
            contactsUseCase.exportAllContacts(outStream.fd).toBlocking().subscribe {
                count ->
                exportedCount = count;
            };
        }

        assertThat(exportedCount, notNullValue());
        assertThat(exportedCount, `is`(3))
        assertThat(tmpFile!!.exists(), `is`(true));

        val importedContacts = contactExchangeFormats.
                importFromContactsJSON(FileUtils.readFileToString(tmpFile));
        assertThat(importedContacts, hasSize(3))
        assertThat(importedContacts, containsInAnyOrder(
                ContactMatcher(contactA), ContactMatcher(contactB),
                ContactMatcher(identityContact)));
    }

}
