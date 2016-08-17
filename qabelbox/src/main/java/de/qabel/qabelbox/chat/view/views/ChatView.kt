package de.qabel.qabelbox.chat.view.views

import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.ui.DataFragment

interface ChatView : DataFragment<ChatMessage> {

    var contactKeyId: String
    var messageText: String

    fun refreshContactOverlay()
    fun sendMessageStateChange()
    fun showError(error : Throwable)

}
