package de.qabel.qabelbox.contacts.view.views

import de.qabel.qabelbox.contacts.dto.ContactDto
import rx.Observable
import java.io.File


interface ContactsView {

    var searchString : Observable<String>
    var showIgnored : Boolean

    fun showEmpty()

    fun loadData(data : List<ContactDto>)

    fun showBottomSheet(contact: ContactDto)

    fun startExportFileChooser(filename: String, requestCode: Int)
    fun startImportFileChooser(requestCode: Int)

    fun startShareDialog(targetFile: File)

    fun showExportFailedMessage()
    fun showExportSuccessMessage(size: Int)

    fun showImportFailedMessage()
    fun showImportSuccessMessage(imported: Int, size: Int)

    fun showContactDeletedMessage(contact: ContactDto)
    fun showContactExistsMessage()

    fun startQRScan()
}

