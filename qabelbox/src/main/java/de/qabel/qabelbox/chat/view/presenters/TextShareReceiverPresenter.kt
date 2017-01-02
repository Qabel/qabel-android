package de.qabel.qabelbox.chat.view.presenters

import de.qabel.qabelbox.contacts.dto.EntitySelection

interface TextShareReceiverPresenter {

    val availableIdentities: List<EntitySelection>
    val contacts: List<EntitySelection>

}
