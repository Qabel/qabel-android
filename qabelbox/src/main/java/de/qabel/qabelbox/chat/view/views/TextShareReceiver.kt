package de.qabel.qabelbox.chat.view.views

import de.qabel.qabelbox.contacts.dto.EntitySelection

interface TextShareReceiver {

    var identity: EntitySelection?
    var contact: EntitySelection?
    var text: String

    fun stop()
    fun showError()

}
