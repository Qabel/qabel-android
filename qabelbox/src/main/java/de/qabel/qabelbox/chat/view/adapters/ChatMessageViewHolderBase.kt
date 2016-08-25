package de.qabel.qabelbox.chat.view.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.helper.Formatter
import kotlinx.android.synthetic.main.chat_message_in.view.*

abstract class ChatMessageViewHolderBase<in T : MessagePayloadDto>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bindTo(message: ChatMessage, showBeginIndicator: Boolean) {
        itemView.chat_begin_indicator?.visibility = if (showBeginIndicator) View.VISIBLE else View.INVISIBLE;
        itemView.setPadding(0, if (showBeginIndicator) 10 else 0, 0, 0);

        itemView.tvDate?.text = Formatter.formatDateTimeString(message.time.time)

        @Suppress("UNCHECKED_CAST")
        bindTo(message.messagePayload as T, message);
    }

    abstract fun bindTo(payload: T, message: ChatMessage)
}

