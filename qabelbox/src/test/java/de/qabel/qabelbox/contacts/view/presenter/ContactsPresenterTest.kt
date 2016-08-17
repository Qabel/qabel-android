package de.qabel.qabelbox.contacts.view.presenter

import com.google.zxing.integration.android.IntentIntegrator
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.core.contacts.ContactExchangeFormats
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.config.QabelSchema
import de.qabel.qabelbox.contacts.ContactsRequestCodes
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase
import de.qabel.qabelbox.contacts.interactor.MainContactsUseCase
import de.qabel.qabelbox.contacts.view.presenters.ContactsPresenter
import de.qabel.qabelbox.contacts.view.presenters.MainContactsPresenter
import de.qabel.qabelbox.contacts.view.views.ContactsView
import de.qabel.qabelbox.external.ExternalAction
import de.qabel.qabelbox.external.ExternalFileAction
import de.qabel.qabelbox.navigation.Navigator
import de.qabel.qabelbox.test.TestConstants
import de.qabel.qabelbox.test.files.FileHelper
import de.qabel.qabelbox.tmp_core.InMemoryContactRepository
import de.qabel.qabelbox.tmp_core.InMemoryIdentityRepository
import de.qabel.qabelbox.util.IdentityHelper
import org.apache.commons.io.FileUtils
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ContactsPresenterTest {

    val identity = IdentityHelper.createIdentity("Identity", TestConstants.PREFIX)
    val contactA = IdentityHelper.createContact("ContactA")
    val contactB = IdentityHelper.createContact("ContactB")

    val contactADto = ContactDto(contactA, listOf(identity))
    val contactBDto = ContactDto(contactB, listOf(), false)

    var contactRepo: ContactRepository = InMemoryContactRepository()
    val identityRepo: IdentityRepository = InMemoryIdentityRepository()

    init {
        identityRepo.save(identity)
        contactRepo.save(contactA, identity)
        contactRepo.save(contactB, identity)
        contactRepo.update(contactB, emptyList())
    }

    lateinit var contactUseCase: ContactsUseCase
    lateinit var presenter: ContactsPresenter

    val contactsView: ContactsView = mock()
    val navigator : Navigator = mock()

    @Before
    fun setUp() {
        whenever(contactsView.showIgnored).thenReturn(false)
        contactUseCase = spy(MainContactsUseCase(identity, contactRepo, identityRepo))
        presenter = MainContactsPresenter(contactsView, contactUseCase,navigator)
    }

    @Test
    fun testRefresh() {
        presenter.refresh()
        verify(contactUseCase).search("", false)
        verify(contactsView).loadData(listOf(contactADto, contactBDto))
    }

    @Test
    fun testSearch() {
        val searchString = "ContactA"
        Mockito.`when`(contactsView.searchString).thenAnswer { searchString }
        presenter.refresh()
        verify(contactUseCase).search(searchString, false)
        verify(contactsView).loadData(listOf(contactADto))
    }

    @Test
    fun testShowIgnored() {
        Mockito.`when`(contactsView.searchString).thenAnswer { "" }
        Mockito.`when`(contactsView.showIgnored).thenAnswer { true }
        presenter.refresh()
        verify(contactUseCase).search("", true)
    }

    @Test
    fun testHandleClick(){
        presenter.handleClick(contactADto)
        verify(navigator).selectChatFragment(contactADto.contact.keyIdentifier)

        presenter.handleClick(contactBDto)
        verify(navigator).selectContactDetailsFragment(contactBDto)
    }

    @Test
    fun testHandleLongClick(){
        presenter.handleLongClick(contactBDto)
        verify(contactsView).showBottomSheet(contactBDto)
    }

    @Test
    fun testDeleteContact() {
        presenter.deleteContact(contactBDto)
        verify(contactUseCase).deleteContact(contactBDto.contact)
        verify(contactsView).showContactDeletedMessage(contactBDto)
        verify(contactsView).loadData(listOf(contactADto))
    }

    @Test
    fun testSendContact() {
        val targetDir = FileHelper.createTmpDir()
        val expectedFile = File(targetDir, QabelSchema.createContactFilename(contactB.alias))
        presenter.sendContact(contactBDto, targetDir)
        verify(contactUseCase).exportContact(contactBDto.contact.keyIdentifier, targetDir)
        verify(contactsView).startShareDialog(expectedFile)
    }

    @Test
    fun testExportContact() {
        val targetDir = FileHelper.createTmpDir()
        val targetFile = File(targetDir, QabelSchema.createContactFilename(contactB.alias))
        presenter.startContactExport(contactBDto)
        assertThat(presenter.externalAction, Matchers.notNullValue())
        assertThat(presenter.externalAction!!.requestCode, equalTo(ContactsRequestCodes.REQUEST_EXPORT_CONTACT))
        assertThat(presenter.externalAction!!.actionType, equalTo(QabelSchema.TYPE_EXPORT_ONE))
        assertThat(presenter.externalAction!!.actionParam, equalTo(contactB.keyIdentifier))
        verify(contactsView).startExportFileChooser(targetFile.name, ContactsRequestCodes.REQUEST_EXPORT_CONTACT)
    }

    @Test
    fun testExportContacts() {
        presenter.startContactsExport()
        assertThat(presenter.externalAction, Matchers.notNullValue())
        assertThat(presenter.externalAction!!.requestCode, equalTo(ContactsRequestCodes.REQUEST_EXPORT_CONTACT))
        assertThat(presenter.externalAction!!.actionType, equalTo(QabelSchema.TYPE_EXPORT_ALL))
        verify(contactsView).startExportFileChooser(QabelSchema.createExportContactsFileName(), ContactsRequestCodes.REQUEST_EXPORT_CONTACT)
    }

    @Test
    fun testImportContacts() {
        presenter.startContactsImport()
        assertThat(presenter.externalAction, notNullValue())
        val externalAction = presenter.externalAction!!
        assertThat(externalAction.requestCode, equalTo(ContactsRequestCodes.REQUEST_IMPORT_CONTACT))
        assertThat(externalAction as? ExternalFileAction, notNullValue())
        assertThat((externalAction as ExternalFileAction).accessMode, `is`("r"))
    }

    @Test
    fun testHandleExternalFileActionImport() {
        contactRepo.delete(contactA, identity)

        val action = ExternalFileAction(ContactsRequestCodes.REQUEST_IMPORT_CONTACT, "w")
        val file = FileHelper.createEmptyTargetFile()
        FileUtils.writeStringToFile(file, ContactExchangeFormats().exportToContactsJSON(setOf(contactA, contactB)))
        FileInputStream(file).use { stream ->
            presenter.handleExternalFileAction(action, stream.fd)
            verify(contactUseCase).importContacts(stream.fd)
        }
        verify(contactsView).showImportSuccessMessage(1, 2)
    }

    @Test
    fun testHandleExternalFileActionExport() {
        val action = ExternalFileAction(ContactsRequestCodes.REQUEST_EXPORT_CONTACT, QabelSchema.TYPE_EXPORT_ONE, contactB.keyIdentifier, "w")
        val file = FileHelper.createEmptyTargetFile()
        FileOutputStream(file).use { stream ->
            presenter.handleExternalFileAction(action, stream.fd)
            verify(contactUseCase).exportContact(contactB.keyIdentifier, stream.fd)
        }
        verify(contactsView).showExportSuccessMessage(1)
    }

    @Test
    fun testHandleScanResult() {
        val action = ExternalAction(IntentIntegrator.REQUEST_CODE, ContactsRequestCodes.REQUEST_QR_IMPORT_CONTACT)
        val contactString = ContactExchangeFormats().exportToContactString(contactA)
        contactRepo.delete(contactA, identity)
        presenter.handleScanResult(action, contactString)
        verify(contactUseCase).importContactString(contactString)
        verify(contactsView).showImportSuccessMessage(1, 1)
    }

    @Test
    fun testStartImportContactScan(){
        presenter.startContactImportScan(0)
        assertThat(presenter.externalAction!!.actionType,
                equalTo(ContactsRequestCodes.REQUEST_QR_IMPORT_CONTACT))
        verify(contactsView).startQRScan()
    }

}
