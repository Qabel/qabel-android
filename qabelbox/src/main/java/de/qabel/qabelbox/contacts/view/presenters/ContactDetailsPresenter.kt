package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.core.config.Identity

interface ContactDetailsPresenter {

    val title : String

    fun refreshContact()

    fun handleEditClick()

    fun onSendMsgClick(identity : Identity)

}
