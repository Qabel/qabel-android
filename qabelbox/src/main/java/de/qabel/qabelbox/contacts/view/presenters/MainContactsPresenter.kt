package de.qabel.qabelbox.contacts.view.presenters

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import com.google.zxing.integration.android.IntentIntegrator
import de.qabel.core.config.Contact
import de.qabel.core.crypto.QblECPublicKey
import de.qabel.core.drop.DropURL
import de.qabel.qabelbox.R
import de.qabel.qabelbox.config.ContactExportImport
import de.qabel.qabelbox.config.QabelSchema
import de.qabel.qabelbox.contacts.ContactsRequestCodes
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase
import de.qabel.qabelbox.contacts.view.ContactsView
import de.qabel.qabelbox.helper.ExternalApps
import de.qabel.qabelbox.helper.UIHelper
import org.apache.commons.io.FileUtils
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import org.spongycastle.util.encoders.Hex
import rx.lang.kotlin.onError
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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

    override fun deleteContact(activity: Activity, contact: ContactDto) {
        UIHelper.showDialogMessage(activity, activity.getString(R.string.dialog_headline_warning),
                activity.getString(R.string.dialog_message_delete_contact_question).
                        replace("%1", contact.contact.alias),
                R.string.yes, R.string.no, DialogInterface.OnClickListener { dialog1, which1 ->
            info { "Deleting contact " + contact.contact.id }
            useCase.deleteContact(contact.contact).subscribe {
                refresh()
                UIHelper.showDialogMessage(activity, R.string.dialog_headline_info,
                        activity.getString(R.string.contact_deleted).
                                replace("%1", contact.contact.alias))
            }
        }, null)
    }

    override fun exportContact(contact: ContactDto) {
        throw UnsupportedOperationException()
    }

    override fun handleActivityResult(activity: Activity, requestCode: Int, resultData: Intent?) {
        if (resultData != null) {
            when (requestCode) {
                ContactsRequestCodes.REQUEST_EXPORT_CONTACT -> {
                    val uri = resultData.data
                    val exportKey = resultData.getStringExtra(ContactsRequestCodes.Params.EXPORT_PARAM)
                    val exportType = resultData.getIntExtra(ContactsRequestCodes.Params.EXPORT_TYPE, 0)
                    activity.contentResolver.openFileDescriptor(uri, "w")!!.use({ pfd ->
                        FileOutputStream(pfd.fileDescriptor).use({ fileOutputStream ->
                            val action = when (exportType) {
                                EXPORT_ONE -> useCase.exportContact(exportKey, fileOutputStream)
                                else -> useCase.exportAllContacts(exportKey, fileOutputStream)
                            };
                            action.onError { throwable ->
                                UIHelper.showDialogMessage(activity,
                                        R.string.dialog_headline_warning,
                                        R.string.contact_export_failed, throwable)
                            }
                            action.subscribe {
                                UIHelper.showDialogMessage(activity, R.string.dialog_headline_info,
                                        activity.resources.getQuantityString(
                                                R.plurals.contact_export_successfully, 1, 1))
                            }
                        })
                    })
                }
                ContactsRequestCodes.REQUEST_IMPORT_CONTACT -> {
                    val uri = resultData.data;
                    val pfd = activity.contentResolver.openFileDescriptor(uri, "r")
                    try {
                        useCase.importContacts(pfd.fileDescriptor)
                                .onError { throwable ->
                                    UIHelper.showDialogMessage(activity,
                                            R.string.dialog_headline_warning,
                                            R.string.contact_import_failed,
                                            throwable)
                                }
                                .subscribe { result ->
                                    if (result.first > 0) {
                                        if (result.first == 1 && result.second == 0) {
                                            UIHelper.showDialogMessage(
                                                    activity,
                                                    activity.getString(R.string.dialog_headline_info),
                                                    activity.resources.getString(R.string.contact_import_successfull))
                                        } else {
                                            UIHelper.showDialogMessage(
                                                    activity,
                                                    activity.getString(R.string.dialog_headline_info),
                                                    activity.resources.getString(R.string.contact_import_successfull_many, result.first,
                                                            result.first + result.second))
                                        }
                                    } else {
                                        UIHelper.showDialogMessage(
                                                activity,
                                                activity.getString(R.string.dialog_headline_info),
                                                activity.getString(R.string.contact_import_zero_additions))
                                    }
                                    refresh()
                                }
                    } catch(exception: Throwable) {
                        exception.printStackTrace();
                    }
                }
            }
        }

        val scanResult = IntentIntegrator.parseActivityResult(requestCode, Activity.RESULT_OK, resultData)
        if (scanResult != null && scanResult.contents != null) {
            debug { "Checking for QR code scan" }
            val result = scanResult.contents.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (result.size == 4 && result[0] == "QABELCONTACT") {
                val dropURL = DropURL(result[2])
                val dropURLs = ArrayList<DropURL>()
                dropURLs.add(dropURL)
                val publicKey = QblECPublicKey(Hex.decode(result[3]))
                val contact = Contact(result[1], dropURLs, publicKey)
                useCase.saveContact(contact)
                        .onError { throwable ->
                            warn("add contact failed", throwable)
                            UIHelper.showDialogMessage(activity, R.string.dialog_headline_warning,
                                    R.string.contact_import_failed, throwable)
                            refresh()
                        }
                        .subscribe { refresh() };

            }
        }
    }

    override fun sendContact(activity: Activity, contact: ContactDto) {
        try {
            val contactJson = ContactExportImport.exportContact(contact.contact)
            val tmpFile = File(activity.externalCacheDir, QabelSchema.createContactFilename(contact.contact.alias))
            FileUtils.writeStringToFile(tmpFile, contactJson)
            ExternalApps.share(activity, Uri.fromFile(tmpFile), "application/json")
        } catch (e: Exception) {
            UIHelper.showDialogMessage(activity, R.string.dialog_headline_warning, R.string.contact_export_failed, e)
        }
    }

}
