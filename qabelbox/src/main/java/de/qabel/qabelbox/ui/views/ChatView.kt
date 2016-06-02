package de.qabel.qabelbox.ui.views

import de.qabel.qabelbox.dto.ChatMessage

interface ChatView {
    fun showEmpty()
    var contactKeyId: String

    fun showMessages(messages: List<ChatMessage>)
}
