package de.qabel.qabelbox.chat.view.presenters

import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.ui.DataViewProxy

interface ChatPresenter {

    val title: String

    val subtitle: String

    val showContactMenu: Boolean

    val proxy: DataViewProxy<ChatMessage>

    fun refreshMessages()

    fun sendMessage()

    fun handleHeaderClick()

    fun handleContactAddClick()

    fun handleContactIgnoreClick()

}
