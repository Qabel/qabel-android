package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.core.config.Contact
import de.qabel.core.crypto.QblECPublicKey
import de.qabel.core.drop.DropURL
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
import org.spongycastle.util.encoders.Hex
import rx.lang.kotlin.onError
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

class MainContactsPresenter @Inject constructor(private val view: ContactsView,
                                                private val useCase: ContactsUseCase) : ContactsPresenter, AnkoLogger {
    var EXPORT_ONE = 1;
    val EXPORT_ALL = 2;

    init {
        refresh()
    }

    override fun refresh() {
        useCase.load().toList().onError {
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

    override fun exportContact(contact: ContactDto) {
        throw UnsupportedOperationException()
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
                EXPORT_ONE -> useCase.exportContact(exportKey, fileOutputStream)
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
                    if (result.first > 0) {
                        if (result.first == 1 && result.second == 0) {
                            view.showMessage(R.string.dialog_headline_info,
                                    R.string.contact_import_successfull)
                        } else {
                            view.showMessage(R.string.dialog_headline_info,
                                    R.string.contact_import_successfull_many,
                                    result.first,
                                    result.first + result.second)
                        }
                    } else {
                        view.showMessage(R.string.dialog_headline_info,
                                R.string.contact_import_zero_additions)
                    }
                    refresh()
                }
    }

    override fun handleScanResult(result: String) {
        val result = result.split("\\r?\\n".toRegex());
        if (result.size == 4 && result[0] == "QABELCONTACT") {
            val dropURL = DropURL(result[2])
            val dropURLs = ArrayList<DropURL>()
            dropURLs.add(dropURL)
            val publicKey = QblECPublicKey(Hex.decode(result[3]))
            val contact = Contact(result[1], dropURLs, publicKey)
            useCase.saveContact(contact)
                    .onError { throwable ->
                        warn("add contact failed", throwable)
                        view.showMessage(R.string.dialog_headline_warning,
                                R.string.contact_import_failed)
                        refresh()
                    }
                    .subscribe {
                        view.showMessage(R.string.dialog_headline_info,
                                R.string.contact_import_successfull)
                        refresh()
                    };
        } else {
            view.showMessage(R.string.dialog_headline_warning,
                    R.string.contact_import_failed)
        }
    }

}
