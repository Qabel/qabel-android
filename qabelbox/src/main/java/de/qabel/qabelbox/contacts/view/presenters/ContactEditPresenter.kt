package de.qabel.qabelbox.contacts.view.presenters

interface ContactEditPresenter {

    val title : String

    fun loadContact()

    fun onSaveClick()

}
