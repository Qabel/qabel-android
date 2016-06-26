package de.qabel.qabelbox.contacts.view.presenters

interface ContactDetailsPresenter {

    val title : String

    open fun refreshContact()
}
