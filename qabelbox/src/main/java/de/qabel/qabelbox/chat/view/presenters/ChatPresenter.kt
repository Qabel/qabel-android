package de.qabel.qabelbox.chat.view.presenters

import de.qabel.core.ui.DataViewProxy
import de.qabel.qabelbox.chat.dto.ChatMessage

interface ChatPresenter {

    val title: String

    val subtitle: String

    val showContactMenu: Boolean

    val proxy: DataViewProxy<ChatMessage>

    fun refreshMessages()

    fun sendMessage()

    fun handleMsgClick(msg : ChatMessage)

    fun handleHeaderClick()

    fun handleContactAddClick()

    fun handleContactIgnoreClick()

}
