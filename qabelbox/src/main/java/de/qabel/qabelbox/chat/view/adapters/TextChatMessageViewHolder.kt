package de.qabel.qabelbox.chat.view.adapters

import android.view.View
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import kotlinx.android.synthetic.main.chat_message_text.view.*

open class TextChatMessageViewHolder(itemView: View) :
        ChatMessageViewHolderBase<MessagePayloadDto.TextMessage>(itemView) {

    override fun bindTo(payload: MessagePayloadDto.TextMessage, message: ChatMessage) {
        itemView.tvText.text = payload.message
    }

}

