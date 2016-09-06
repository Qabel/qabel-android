package de.qabel.qabelbox.repositories

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.test.RenamingDelegatingContext
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.config.factory.DropUrlGenerator
import de.qabel.core.config.factory.IdentityBuilder
import de.qabel.core.crypto.QblECPublicKey
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.EntityManager
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.exception.EntityExistsException
import de.qabel.core.repository.exception.EntityNotFoundException
import de.qabel.qabelbox.persistence.RepositoryFactory
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class SqliteContactRepositoryTest {

    private lateinit var contactRepo: ContactRepository

    private lateinit var repositoryFactory: RepositoryFactory
    private lateinit var entityManager: EntityManager

    private lateinit var identity: Identity
    private lateinit var otherIdentity: Identity
    private lateinit var contact: Contact
    private lateinit var otherContact: Contact
    private lateinit var unknownContact: Contact
    private lateinit var ignoredContact: Contact
    private lateinit var pubKey: QblECPublicKey
    private lateinit var identityRepository: IdentityRepository
    private lateinit var dropUrlGenerator: DropUrlGenerator

    @Before
    fun setUp() {

        val context = RenamingDelegatingContext(
                InstrumentationRegistry.getInstrumentation().targetContext, "factorytest_" + System.currentTimeMillis())

        repositoryFactory = RepositoryFactory(context)
        repositoryFactory.deleteDatabase()

        identity = IdentityBuilder(DropUrlGenerator("http://localhost")).withAlias("tester").build()
        otherIdentity = IdentityBuilder(DropUrlGenerator("http://localhost")).withAlias("other i").build()
        pubKey = QblECPublicKey("test".toByteArray())
        contact = Contact("testcontact", mutableListOf(), pubKey)
        otherContact = Contact("other contact", mutableListOf(), QblECPublicKey("test2".toByteArray()))
        unknownContact = Contact("other contact", mutableListOf(), QblECPublicKey("test3".toByteArray())).apply { status = Contact.ContactStatus.UNKNOWN }
        ignoredContact = Contact("other contact", mutableListOf(), QblECPublicKey("test4".toByteArray())).apply { isIgnored = true }

        entityManager = repositoryFactory.entityManager
        identityRepository = repositoryFactory.getIdentityRepository()
        contactRepo = repositoryFactory.getContactRepository()

        identityRepository.save(identity)
        identityRepository.save(otherIdentity)
        dropUrlGenerator = DropUrlGenerator("http://localhost")
    }

    @Test(expected = EntityNotFoundException::class)
    fun throwsExceptionWhenNotFound() {
        contactRepo.findByKeyId(identity, pubKey.readableKeyIdentifier)
    }

    @Test
    fun findsSavedContact() {
        contactRepo.save(contact, identity)
        val loaded = contactRepo.findByKeyId(identity, contact.keyIdentifier)
        assertSame(loaded, contact)
    }

    @Test
    fun loadsUncachedContact() {
        contact.phone = "01234567890"
        contact.email = "test@test.de"
        contactRepo.save(contact, identity)
        entityManager.clear()

        val loaded = contactRepo.findByKeyId(identity, contact.keyIdentifier)

        assertEquals(contact.keyIdentifier, loaded.keyIdentifier)
        assertEquals(contact.alias, loaded.alias)
        assertEquals("01234567890", loaded.phone)
        assertEquals("test@test.de", loaded.email)
    }

    @Test
    fun alwaysLoadsSameInstance() {
        contactRepo.save(contact, identity)
        val instance1 = contactRepo.findByKeyId(identity, contact.keyIdentifier)
        val instance2 = contactRepo.findByKeyId(identity, contact.keyIdentifier)
        assertSame(instance1, instance2)
    }

    @Test
    fun persistsDropUrls() {
        contact.addDrop(dropUrlGenerator.generateUrl())
        contactRepo.save(contact, identity)
        entityManager.clear()

        val loaded = contactRepo.findByKeyId(identity, contact.keyIdentifier)
        compareDropUrls(loaded)
    }

    fun compareDropUrls(loaded: Contact) {
        var dropUrls = contact.dropUrls
        val originalUrls = LinkedList(dropUrls)
        dropUrls = loaded.dropUrls
        val loadedUrls = LinkedList(dropUrls)
        Collections.sort(originalUrls) { o1, o2 -> o1.toString().compareTo(o2.toString()) }
        Collections.sort(loadedUrls) { o1, o2 -> o1.toString().compareTo(o2.toString()) }

        assertTrue(
                "DropUrls not persisted/loaded: $loadedUrls != $originalUrls",
                Arrays.equals(originalUrls.toTypedArray(), loadedUrls.toTypedArray()))
    }

    @Test
    fun updatesEntries() {
        contact.addDrop(dropUrlGenerator.generateUrl())
        contactRepo.save(contact, identity)

        contact.alias = "new alias"
        contact.email = "new mail"
        contact.phone = "666"
        contact.addDrop(dropUrlGenerator.generateUrl())
        contactRepo.save(contact, identity)
        entityManager.clear()

        val loaded = contactRepo.findByKeyId(identity, contact.keyIdentifier)

        assertEquals(contact.id.toLong(), loaded.id.toLong())
        assertEquals(contact.alias, loaded.alias)
        assertEquals(contact.email, loaded.email)
        assertEquals(contact.phone, loaded.phone)
        compareDropUrls(loaded)
    }

    @Test
    fun providesEmptyContactListByDefault() {
        val contacts = contactRepo.find(identity)
        assertEquals(0, contacts.contacts.size.toLong())
        assertSame(identity, contacts.identity)
    }

    @Test
    fun findsMatchingContact() {
        contactRepo.save(contact, identity)
        val contacts = contactRepo.find(identity)
        assertEquals(1, contacts.contacts.size.toLong())
        assertSame(contact, contacts.contacts.toTypedArray()[0])
    }

    @Test
    fun ignoresNotMatchingContacts() {
        contactRepo.save(contact, identity)
        contactRepo.save(otherContact, otherIdentity)
        val contacts = contactRepo.find(otherIdentity)
        assertEquals(1, contacts.contacts.size.toLong())
        assertSame(otherContact, contacts.contacts.toTypedArray()[0])
    }

    @Test
    fun deletesContact() {
        contactRepo.save(contact, identity)
        contactRepo.delete(contact, identity)

        try {
            contactRepo.findByKeyId(identity, contact.keyIdentifier)
            fail("entity was not deleted")
        } catch (ignored: EntityNotFoundException) {
        }

    }

    @Test
    fun deletesTheCorrelatedContactOnly() {
        contactRepo.save(contact, identity)
        contactRepo.save(contact, otherIdentity)
        contactRepo.delete(contact, identity)

        try {
            contactRepo.findByKeyId(identity, contact.keyIdentifier)
            fail("connection from contact to identity was not deleted")
        } catch (ignored: EntityNotFoundException) {
        }

        val loaded = contactRepo.findByKeyId(otherIdentity, contact.keyIdentifier)
        assertSame(contact, loaded)
    }

    @Test
    fun reAddedContactKeepsSameInstance() {
        contactRepo.save(contact, identity)
        contactRepo.delete(contact, identity)
        contactRepo.save(contact, identity)

        val loaded = contactRepo.findByKeyId(identity, contact.keyIdentifier)
        assertSame(contact, loaded)
    }

    @Test
    fun addsRelationshipIfContactIsAlreadyPresent() {
        contactRepo.save(contact, identity)

        val newImport = Contact(contact.alias, contact.dropUrls, contact.ecPublicKey)
        contactRepo.save(newImport, otherIdentity)
    }

    @Test
    fun multipleContactsArePossible() {
        contactRepo.save(contact, identity)
        contactRepo.save(otherContact, identity)

        val contacts = contactRepo.find(identity)
        assertThat(contacts.contacts, hasSize<Any>(2))
    }

    @Test
    fun testFindAll() {
        contactRepo.save(contact, identity)
        contactRepo.update(contact, listOf(identity, otherIdentity))
        contactRepo.save(otherContact, identity)
        contactRepo.update(otherContact, emptyList())

        //Sorted by name with associated identities
        val storedContacts = listOf(
                Pair(otherContact, emptyList()),
                Pair(contact, listOf(identity, otherIdentity)))

        val contacts = contactRepo.findWithIdentities()

        //Check content and order
        contacts.forEachIndexed { i, contactDetails ->
            val storedDto = storedContacts[i]
            assertThat(storedDto, notNullValue())
            assertThat(storedDto.first.alias, equalTo(contactDetails.contact.alias))
            assertThat(storedDto.second, hasSize(contactDetails.identities.size))
        }
        assertThat(storedContacts, hasSize(contacts.size))
    }

    @Test
    fun testFindAllFiltered() {
        contactRepo.save(contact, identity)
        contactRepo.save(contact, otherIdentity)
        contactRepo.save(otherContact, identity)

        val filter = "other c"
        val contacts = contactRepo.findWithIdentities(filter)
        assertEquals(1, contacts.size)

        val fooContactDetails = contacts.first()
        assertThat(otherContact.alias, equalTo(fooContactDetails.contact.alias))
        assertThat(1, equalTo(fooContactDetails.identities.size))
    }

    @Test
    fun testFindAllWithDefaults() {
        contactRepo.save(contact, identity)
        contactRepo.save(unknownContact, identity)
        contactRepo.save(ignoredContact, identity)
        contactRepo.save(otherContact, identity)

        //Use defaults
        val contacts = contactRepo.findWithIdentities()

        assertEquals(2, contacts.size)

        val fooContact = contacts.first()
        assertThat(otherContact.alias, equalTo(fooContact.contact.alias))
        assertThat(1, equalTo(fooContact.identities.size))
    }

    @Test
    fun testFindAllIgnored() {
        contactRepo.save(contact, identity)
        contactRepo.save(unknownContact, identity)
        contactRepo.save(ignoredContact, identity)
        contactRepo.save(otherContact, identity)

        val contacts = contactRepo.findWithIdentities("", listOf(Contact.ContactStatus.NORMAL), false)
        assertEquals(3, contacts.size)
        val igContact = contacts.find { it.contact.id == ignoredContact.id }!!
        assertThat(ignoredContact.alias, equalTo(igContact.contact.alias))
        assertThat(1, equalTo(igContact.identities.size))
    }

    @Test
    fun testUpdate() {
        val dropGen = DropUrlGenerator("http://mock.de")
        val dropA = dropGen.generateUrl()
        val dropB = dropGen.generateUrl()
        contact.addDrop(dropA)
        contactRepo.save(contact, identity)
        contact.addDrop(dropB)
        contact.nickName = "testNick"
        contactRepo.update(contact, listOf(identity, otherIdentity))

        val result = contactRepo.findContactWithIdentities(contact.keyIdentifier)
        assertThat(result.contact.nickName, equalTo("testNick"))
        assertThat(result.identities, containsInAnyOrder(identity, otherIdentity))
        assertThat(contact.dropUrls, containsInAnyOrder(dropA, dropB))
    }

}
