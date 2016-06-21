package de.qabel.qabelbox.contacts.view

import de.qabel.qabelbox.contacts.dto.ContactDto


interface ContactsView {

    fun showEmpty()

    fun loadData(data : List<ContactDto>)

    fun showMessage(title : Int, message : Int)
    fun showMessage(title : Int, message : Int, paramA : Any?, paramB : Any?)
    fun showQuantityMessage(title : Int, message : Int, quantity : Int, vararg params : Any)
    fun showConfirmation(title : Int, message: Int, params: Any, yesClick : () -> Unit)

}

