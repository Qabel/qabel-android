package de.qabel.qabelbox.contacts.view.views

import de.qabel.qabelbox.contacts.dto.ContactDto


interface ContactsView {

    var searchString : String?

    fun showEmpty()

    fun loadData(data : List<ContactDto>)

    fun showMessage(title : Int, message : Int)
    fun showMessage(title : Int, message : Int, paramA : Any?, paramB : Any?)
    fun showQuantityMessage(title : Int, message : Int, quantity : Int, param : Any?)
    fun showConfirmation(title : Int, message: Int, params: Any, yesClick : () -> Unit)

    fun startExportFileChooser(filename: String, requestCode: Int)
    fun startImportFileChooser(requestCode: Int)

}

