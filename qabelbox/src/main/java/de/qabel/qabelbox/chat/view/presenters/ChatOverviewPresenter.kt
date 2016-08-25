package de.qabel.qabelbox.chat.view.presenters

import de.qabel.qabelbox.chat.dto.ChatMessage

interface ChatOverviewPresenter {

    fun refresh()

    fun handleClick(message : ChatMessage)
    fun handleLongClick(message : ChatMessage) : Boolean

    fun navigateToContacts()

}
