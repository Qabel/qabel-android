package de.qabel.qabelbox.chat.view.views

import de.qabel.qabelbox.chat.dto.ChatMessage

interface ChatView {
    fun showEmpty()
    var contactKeyId: String
    var messageText: String

    fun showMessages(messages: List<ChatMessage>)

    fun appendMessage(message: ChatMessage)
    open fun sendMessageStateChange()
}
