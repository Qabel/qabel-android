package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.core.repository.exception.EntityExistsException
import de.qabel.qabelbox.config.QabelSchema
import de.qabel.qabelbox.contacts.ContactsRequestCodes
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase
import de.qabel.qabelbox.contacts.view.views.ContactsView
import de.qabel.qabelbox.external.ExternalAction
import de.qabel.qabelbox.external.ExternalFileAction
import de.qabel.qabelbox.navigation.Navigator
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import java.io.File
import java.io.FileDescriptor
import javax.inject.Inject

class MainContactsPresenter @Inject constructor(private val view: ContactsView,
                                                private val useCase: ContactsUseCase,
                                                private val navigator: Navigator) : ContactsPresenter, AnkoLogger {

    //TODO Store external action in Bundle
    override var externalAction: ExternalAction? = null

    private var searchString: String = ""

    init {
        view.searchString.subscribe {
            searchString = it
            refresh()
        }
    }

    override fun refresh() {
        useCase.search(searchString, view.showIgnored).toList().subscribe({ contacts ->
            if (contacts.size > 0) {
                view.loadData(contacts)
            } else (view.showEmpty())
        }, { throwable ->
            warn("Cant load contacts", throwable)
            view.showEmpty()
        })
    }

    override fun handleClick(contact: ContactDto) =
            if (!contact.contact.isIgnored && contact.active)
                navigator.selectChatFragment(contact.contact.keyIdentifier)
            else navigator.selectContactDetailsFragment(contact)

    override fun handleLongClick(contact: ContactDto): Boolean {
        view.showBottomSheet(contact)
        return true
    }

    override fun deleteContact(contact: ContactDto) {
        info("Deleting contact " + contact.contact.id)
        useCase.deleteContact(contact.contact).subscribe({
            view.showContactDeletedMessage(contact)
            refresh()
        })
    }

    override fun sendContact(contact: ContactDto, cacheDir: File) {
        useCase.exportContact(contact.contact.keyIdentifier, cacheDir)
                .subscribe({ exportedContactFile ->
                    view.startShareDialog(exportedContactFile)
                }, {
                    view.showExportFailedMessage()
                })
    }

    override fun handleExternalFileAction(externalAction: ExternalAction, target: FileDescriptor) {
        when (externalAction.requestCode) {
            ContactsRequestCodes.REQUEST_EXPORT_CONTACT -> handleContactExport(externalAction, target)
            ContactsRequestCodes.REQUEST_IMPORT_CONTACT -> handleContactImport(target)
            else -> warn("Unknown external file action detected!")
        }
    }

    private fun handleContactExport(externalAction: ExternalAction, target: FileDescriptor) {
        val exportAction = when (externalAction.actionType) {
            QabelSchema.TYPE_EXPORT_ONE -> useCase.exportContact(externalAction.actionParam as String, target)
            else -> useCase.exportAllContacts(target)
        }
        exportAction.subscribe({ exportedCount ->
            view.showExportSuccessMessage(exportedCount)
        }, { throwable ->
            view.showExportFailedMessage()
        })
    }

    private fun handleContactImport(target: FileDescriptor) {
        useCase.importContacts(target)
                .subscribe({ result ->
                    view.showImportSuccessMessage(result.successCount,
                            result.successCount + result.failedCount)
                    refresh()
                }, { throwable ->
                    view.showExportFailedMessage()
                })
    }

    override fun handleScanResult(externalAction: ExternalAction, result: String) {
        when (externalAction.actionType) {
            ContactsRequestCodes.REQUEST_QR_IMPORT_CONTACT -> {
                useCase.importContactString(result).subscribe({
                    view.showImportSuccessMessage(1, 1)
                    refresh()
                }, { throwable ->
                    when (throwable) {
                        is EntityExistsException -> view.showContactExistsMessage()
                        else -> view.showImportFailedMessage()
                    }
                })
            }
            ContactsRequestCodes.REQUEST_QR_VERIFY_CONTACT -> {
                //TODO Coming soon
            }
        }
    }

    override fun startContactExport(contact: ContactDto) {
        externalAction = ExternalFileAction(ContactsRequestCodes.REQUEST_EXPORT_CONTACT, QabelSchema.TYPE_EXPORT_ONE, contact.contact.keyIdentifier, "w")
        view.startExportFileChooser(QabelSchema.createContactFilename(contact.contact.alias), ContactsRequestCodes.REQUEST_EXPORT_CONTACT)
    }

    override fun startContactsExport() {
        externalAction = ExternalFileAction(ContactsRequestCodes.REQUEST_EXPORT_CONTACT, QabelSchema.TYPE_EXPORT_ALL, null, "w")
        view.startExportFileChooser(QabelSchema.createExportContactsFileName(), ContactsRequestCodes.REQUEST_EXPORT_CONTACT)
    }

    override fun startContactsImport() {
        externalAction = ExternalFileAction(ContactsRequestCodes.REQUEST_IMPORT_CONTACT, "r")
        view.startImportFileChooser(ContactsRequestCodes.REQUEST_IMPORT_CONTACT)
    }

    override fun startContactImportScan(requestCode: Int) {
        externalAction = ExternalAction(requestCode, ContactsRequestCodes.REQUEST_QR_IMPORT_CONTACT)
        view.startQRScan()
    }

}
