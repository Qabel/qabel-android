package de.qabel.qabelbox.chat.view.adapters

import android.view.View
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayload
import kotlinx.android.synthetic.main.chat_message_text.view.*

open class TextChatMessageViewHolder(itemView: View) :
        ChatMessageViewHolderBase<MessagePayload.TextMessage>(R.layout.chat_message_text, itemView) {

    override fun bindTo(payload: MessagePayload.TextMessage, message: ChatMessage) {
        itemView.tvText.text = payload.message
    }

}

