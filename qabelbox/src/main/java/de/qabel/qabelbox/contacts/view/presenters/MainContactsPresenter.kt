package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.qabelbox.R
import de.qabel.qabelbox.config.ContactExportImport
import de.qabel.qabelbox.config.QabelSchema
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase
import de.qabel.qabelbox.contacts.view.ContactsView
import org.apache.commons.io.FileUtils
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import rx.lang.kotlin.onError
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import javax.inject.Inject

class MainContactsPresenter @Inject constructor(private val view: ContactsView,
                                                private val useCase: ContactsUseCase) : ContactsPresenter, AnkoLogger {

    init {
        refresh()
    }

    override fun refresh() {
        (if (view.searchString != null) useCase.search(view.searchString as String)
        else useCase.load())
                .toList().onError {
            view.showEmpty()
        }.subscribe({ contacts ->
            if (contacts.size > 0) {
                view.loadData(contacts)
            } else (view.showEmpty())
        })
    }

    override fun deleteContact(contact: ContactDto) {
        view.showConfirmation(R.string.dialog_headline_warning,
                R.string.dialog_message_delete_contact_question,
                contact.contact.alias,
                {
                    info { "Deleting contact " + contact.contact.id }
                    view.showMessage(R.string.dialog_headline_info,
                            R.string.contact_deleted,
                            contact.contact.alias, Unit);
                    refresh()
                })
    }

    override fun sendContact(contact: ContactDto, cacheDir: File): File? {
        var targetFile: File? = null;
        try {
            val contactJson = ContactExportImport.exportContact(contact.contact)
            targetFile = File(cacheDir, QabelSchema.createContactFilename(contact.contact.alias))
            FileUtils.writeStringToFile(targetFile, contactJson)
        } catch (e: Exception) {
            view.showMessage(R.string.dialog_headline_warning, R.string.contact_export_failed)
        }
        return targetFile;
    }

    override fun exportContacts(exportType: Int, exportKey: String, target: FileDescriptor) {
        FileOutputStream(target).use({ fileOutputStream ->
            val action = when (exportType) {
                QabelSchema.TYPE_EXPORT_ONE -> useCase.exportContact(exportKey, fileOutputStream)
                else -> useCase.exportAllContacts(exportKey, fileOutputStream)
            };
            action.onError { throwable ->
                view.showMessage(R.string.dialog_headline_warning, R.string.contact_export_failed);
            }
            action.subscribe {
                view.showQuantityMessage(R.string.dialog_headline_info,
                        R.plurals.contact_export_successfully, 1, 1)
            }
        })
    }

    override fun importContacts(target: FileDescriptor) {
        useCase.importContacts(target)
                .onError { throwable ->
                    view.showMessage(
                            R.string.dialog_headline_warning,
                            R.string.contact_import_failed)
                }
                .subscribe { result ->
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
                }
    }

    override fun handleScanResult(result: String) {
        useCase.importContactString(result).doOnCompleted {
            view.showMessage(R.string.dialog_headline_info,
                    R.string.contact_import_successfull)
            refresh()
        }.onError { throwable ->
            warn("add contact failed", throwable)
            view.showMessage(R.string.dialog_headline_warning,
                    R.string.contact_import_failed)
            refresh()
        }.subscribe { };
    }

}
