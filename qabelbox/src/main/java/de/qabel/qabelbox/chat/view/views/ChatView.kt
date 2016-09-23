package de.qabel.qabelbox.chat.view.views

import de.qabel.core.ui.DataView
import de.qabel.qabelbox.box.provider.ShareId
import de.qabel.qabelbox.chat.dto.ChatMessage

interface ChatView : DataView<ChatMessage> {

    var contactKeyId: String
    var messageText: String

    fun refreshContactOverlay()
    fun sendMessageStateChange()
    fun showError(error : Throwable)

    open fun openShare(shareId: ShareId)
}
