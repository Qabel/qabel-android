package de.qabel.qabelbox.contacts.interactor

import de.qabel.core.config.Contact
import de.qabel.core.contacts.ContactExchangeFormats
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.inmemory.InMemoryContactRepository
import de.qabel.core.repository.inmemory.InMemoryIdentityRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.config.QabelSchema
import de.qabel.qabelbox.contacts.ContactMatcher
import de.qabel.qabelbox.contacts.dto.ContactDto
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


    lateinit var contactRepo: ContactRepository
    lateinit var contactsUseCase: ContactsUseCase

    @Before
    fun setUp() {
        contactRepo = InMemoryContactRepository()
        contactsUseCase = MainContactsUseCase(identityA, contactRepo, InMemoryIdentityRepository())
    }

    @Test
    fun testLoad() {
        val contacts = contactsUseCase.search("", false).toList().toBlocking().first()
        assertThat(contacts, hasSize(0))
    }

    @Test
    fun testLoad1() {
        contactRepo.save(contactA, identityA)
        val contacts = contactsUseCase.search("", false).toList().toBlocking().first()
        assertThat(contacts, hasSize(1))
        val loadedContact = contacts!!.first()
        assertThat(loadedContact.contact, equalTo(contactA))
        assertThat(loadedContact.identities, hasSize(1))
        assertThat(loadedContact.identities.first(), equalTo(identityA))
    }

    @Test
    fun testLoad2() {
        contactRepo.save(contactA, identityA)
        contactRepo.save(contactA, identityB)
        contactRepo.save(contactB, identityB)
        val contacts = contactsUseCase.search("", false).toList().toBlocking().first()
        val storedA = contacts.find { c -> c.contact.keyIdentifier.equals(contactA.keyIdentifier) }!!
        val storedB = contacts.find { c -> c.contact.keyIdentifier.equals(contactB.keyIdentifier) }!!
        assertThat(contacts, hasSize(2))

        assertThat(storedA.contact, equalTo(contactA))
        assertThat(storedA.identities, hasSize(2))
        assertThat(storedA.active, `is`(true))
        assertThat(storedA.identities, containsInAnyOrder(identityA, identityB))

        assertThat(storedB.contact, equalTo(contactB))
        assertThat(storedB.identities, hasSize(1))
        assertThat(storedB.active, `is`(false))
        assertThat(storedB.identities, containsInAnyOrder(identityB))
    }

    @Test
    fun testSearch() {
        contactRepo.save(contactA, identityA)
        contactRepo.save(contactA, identityB)
        contactRepo.save(identityContact, identityB)

        val contacts = contactsUseCase.search("identity", false).toList().toBlocking().first()
        assertThat(contacts, hasSize(1))
        val foundContact = contacts!!.first()
        assertThat(foundContact.contact, equalTo(identityContact))
        assertThat(foundContact.identities, hasSize(1))
        assertThat(foundContact.identities.first(), equalTo(identityB))
    }

    @Test
    fun testLoadIgnored(){
        contactRepo.save(contactA, identityA)
        contactB.isIgnored = true
        contactRepo.save(contactB, identityA)
        val contacts = contactsUseCase.search("", true).toList().toBlocking().first()
        assertThat(contacts, hasSize(2))
        assertThat(contacts.find { it.contact.id == contactA.id }, notNullValue())
        assertThat(contacts.find { it.contact.id == contactB.id }, notNullValue())
    }

    @Test
    fun testLoadContact() {
        contactRepo.save(contactA, identityA)
        contactRepo.save(contactA, identityB)

        val contact = contactsUseCase.loadContact(contactA.keyIdentifier).toBlocking().first()
        assertThat(contact.active, `is`(true))
        assertThat(contact.contact, equalTo(contactA))
        assertThat(contact.identities, hasSize(2))
        assertThat(contact.identities, containsInAnyOrder(identityA, identityB))
    }

    @Test
    fun testSaveContact() {
        contactRepo.save(contactA, identityA)
        val contactBDto = ContactDto(contactB, listOf(identityB))
        contactsUseCase.saveContact(contactBDto).toBlocking().subscribe {}

        val resultContact = contactRepo.find(identityB)
        assertThat(resultContact.getByKeyIdentifier(contactB.keyIdentifier), equalTo(contactB))
    }

    @Test
    fun testSaveUnknownContact() {
        contactB.status = Contact.ContactStatus.UNKNOWN
        val contactBDto = ContactDto(contactB, listOf(identityB))
        contactsUseCase.saveContact(contactBDto).toBlocking().subscribe {}

        val resultDto = contactsUseCase.loadContact(contactB.keyIdentifier).toBlocking().first()

        assertThat(Contact.ContactStatus.NORMAL, equalTo(resultDto.contact.status))
        assertThat(resultDto.identities, hasSize(1))
    }

    @Test
    fun testDeleteContact() {
        contactRepo.save(contactA, identityA)
        contactRepo.save(contactB, identityA)
        contactsUseCase.deleteContact(contactB).toBlocking().subscribe {}
        assertThat(contactRepo.exists(contactB), `is`(false))
    }

    @Test
    fun testExportContactToDirectory() {
        contactRepo.save(contactA, identityA)

        val contactExchangeFormats = ContactExchangeFormats()
        val tmpFolder = FileHelper.createTmpDir()

        var exportedContactFile: File? = null
        contactsUseCase.exportContact(contactA.keyIdentifier, tmpFolder).toBlocking().subscribe {
            resultFile ->
            exportedContactFile = resultFile
        }

        assertThat(exportedContactFile, notNullValue())
        assertThat(exportedContactFile!!.exists(), `is`(true))
        assertThat(listOf(*tmpFolder.listFiles()), containsInAnyOrder(exportedContactFile))
        assertThat(exportedContactFile!!.name, equalTo(QabelSchema.createContactFilename(contactA.alias)))

        val importedContacts = contactExchangeFormats.
                importFromContactsJSON(FileUtils.readFileToString(exportedContactFile))
        assertThat(importedContacts, hasSize(1))
        assertThat(contactA, ContactMatcher(importedContacts.first()))
    }

    @Test
    fun testExportContactToFile() {
        contactRepo.save(contactA, identityA)
        contactRepo.save(contactB, identityA)

        val contactExchangeFormats = ContactExchangeFormats()
        val tmpFile = FileHelper.createEmptyTargetFile()

        var exportedCount: Int? = null
        FileOutputStream(tmpFile).use { outStream ->
            contactsUseCase.exportContact(contactB.keyIdentifier, outStream.fd).toBlocking().subscribe {
                count ->
                exportedCount = count
            }
        }
        assertThat(exportedCount, `is`(1))

        val importedContacts = contactExchangeFormats.
                importFromContactsJSON(FileUtils.readFileToString(tmpFile))
        assertThat(importedContacts, hasSize(1))
        assertThat(importedContacts.first(), ContactMatcher(contactB))
    }

    @Test
    fun testExportAllContactsToFile() {
        contactRepo.save(contactA, identityA)
        contactRepo.save(contactB, identityA)
        contactRepo.save(identityContact, identityA)

        val contactExchangeFormats = ContactExchangeFormats()
        val tmpFile = FileHelper.createEmptyTargetFile()

        var exportedCount: Int? = null
        FileOutputStream(tmpFile).use { outStream ->
            contactsUseCase.exportAllContacts(outStream.fd).toBlocking().subscribe {
                count ->
                exportedCount = count
            }
        }

        assertThat(exportedCount, notNullValue())
        assertThat(exportedCount, `is`(3))
        assertThat(tmpFile!!.exists(), `is`(true))

        val importedContacts = contactExchangeFormats.
                importFromContactsJSON(FileUtils.readFileToString(tmpFile))
        assertThat(importedContacts, hasSize(3))
        assertThat(importedContacts, containsInAnyOrder(
                ContactMatcher(contactA), ContactMatcher(contactB),
                ContactMatcher(identityContact)))
    }

}
