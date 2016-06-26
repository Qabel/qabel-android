package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.qabelbox.R
import de.qabel.qabelbox.config.ContactExportImport
import de.qabel.qabelbox.config.QabelSchema
import de.qabel.qabelbox.contacts.ContactsRequestCodes
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase
import de.qabel.qabelbox.contacts.view.ContactsView
import de.qabel.qabelbox.external.ExternalAction
import de.qabel.qabelbox.external.ExternalFileAction
import org.apache.commons.io.FileUtils
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import java.io.File
import java.io.FileDescriptor
import javax.inject.Inject

class MainContactsPresenter @Inject constructor(private val view: ContactsView,
                                                private val useCase: ContactsUseCase) : ContactsPresenter, AnkoLogger {

    override var externalAction: ExternalAction? = null;

    init {
        refresh()
    }


    override fun refresh() {
        (if (view.searchString != null) useCase.search(view.searchString as String)
        else useCase.load())
                .toList().subscribe({ contacts ->
            if (contacts.size > 0) {
                view.loadData(contacts)
            } else (view.showEmpty())
        }, { throwable ->
            warn("Cant load contacts", throwable);
            view.showEmpty()
        })
    }

    override fun deleteContact(contact: ContactDto) {
        view.showConfirmation(R.string.dialog_headline_warning,
                R.string.dialog_message_delete_contact_question,
                contact.contact.alias,
                {
                    info("Deleting contact " + contact.contact.id);
                    useCase.deleteContact(contact.contact).subscribe({
                        view.showMessage(R.string.dialog_headline_info,
                                R.string.contact_deleted,
                                contact.contact.alias, Unit);
                        refresh()
                    })
                })
    }

    override fun sendContact(contact: ContactDto, cacheDir: File): File? {
        var targetFile: File? = null;
        try {
            //TODO Move to usecase
            val contactJson = ContactExportImport.exportContact(contact.contact)
            targetFile = File(cacheDir, QabelSchema.createContactFilename(contact.contact.alias))
            FileUtils.writeStringToFile(targetFile, contactJson)
        } catch (e: Exception) {
            view.showMessage(R.string.dialog_headline_warning, R.string.contact_export_failed)
        }
        return targetFile;
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
        };
        exportAction.subscribe({ exportedCount ->
            view.showQuantityMessage(R.string.dialog_headline_info,
                    R.plurals.contact_export_successfully, exportedCount, exportedCount)
        }, { throwable ->
            view.showMessage(R.string.dialog_headline_warning, R.string.contact_export_failed);
        })
    }

    private fun handleContactImport(target: FileDescriptor) {
        useCase.importContacts(target)
                .subscribe({ result ->
                    if (result.successCount > 0) {
                        if (result.successCount == 1 && result.failedCount == 0) {
                            view.showMessage(R.string.dialog_headline_info,
                                    R.string.contact_import_successfull)
                        } else {
                            view.showMessage(R.string.dialog_headline_info,
                                    R.string.contact_import_successfull_many,
                                    result.successCount,
                                    result.successCount + result.failedCount)
                        }
                    } else {
                        view.showMessage(R.string.dialog_headline_info,
                                R.string.contact_import_zero_additions)
                    }
                    refresh()
                }, { throwable ->
                    view.showMessage(
                            R.string.dialog_headline_warning,
                            R.string.contact_import_failed)
                })
    }

    override fun handleScanResult(externalAction: ExternalAction, result: String) {
        when (externalAction.requestCode) {
            ContactsRequestCodes.REQUEST_QR_IMPORT_CONTACT -> {
                useCase.importContactString(result).subscribe({
                    view.showMessage(R.string.dialog_headline_info,
                            R.string.contact_import_successfull)
                    refresh()
                }, { throwable ->
                    view.showMessage(R.string.dialog_headline_warning, R.string.contact_export_failed);
                });
            }
            ContactsRequestCodes.REQUEST_QR_VERIFY_CONTACT -> {
                //TODO Coming soon
            }
        }
    }

    override fun startContactExport(contact: ContactDto) {
        externalAction = ExternalFileAction(ContactsRequestCodes.REQUEST_EXPORT_CONTACT, QabelSchema.TYPE_EXPORT_ONE, contact.contact.keyIdentifier, "w");
        view.startExportFileChooser(QabelSchema.createContactFilename(contact.contact.alias), ContactsRequestCodes.REQUEST_EXPORT_CONTACT);
    }

    override fun startContactsExport() {
        externalAction = ExternalFileAction(ContactsRequestCodes.REQUEST_EXPORT_CONTACT, QabelSchema.TYPE_EXPORT_ALL, null, "w");
        view.startExportFileChooser(QabelSchema.createExportContactsFileName(), ContactsRequestCodes.REQUEST_EXPORT_CONTACT);
    }

    override fun startContactsImport() {
        externalAction = ExternalFileAction(ContactsRequestCodes.REQUEST_IMPORT_CONTACT, "r");
        view.startImportFileChooser(ContactsRequestCodes.REQUEST_IMPORT_CONTACT);
    }

}
