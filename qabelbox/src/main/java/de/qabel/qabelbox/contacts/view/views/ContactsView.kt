package de.qabel.qabelbox.contacts.view.views

import de.qabel.qabelbox.contacts.dto.ContactDto
import java.io.File


interface ContactsView {

    var searchString : String?

    fun showEmpty()

    fun loadData(data : List<ContactDto>)

    fun showMessage(title : Int, message : Int)
    fun showMessage(title : Int, message : Int, vararg messageParams : Any?)
    fun showQuantityMessage(title : Int, message : Int, quantity : Int, vararg messageParams : Any?)
    fun showConfirmation(title : Int, message: Int, params: Any, yesClick : () -> Unit)

    fun startExportFileChooser(filename: String, requestCode: Int)
    fun startImportFileChooser(requestCode: Int)

    open fun startShareDialog(targetFile: File)
}

