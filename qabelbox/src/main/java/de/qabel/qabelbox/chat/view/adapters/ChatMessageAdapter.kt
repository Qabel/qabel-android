package de.qabel.qabelbox.chat.view.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayload

open class ChatMessageAdapter(var messages: List<ChatMessage>) : RecyclerView.Adapter<ChatMessageViewHolderBase<*>>() {

    enum class MessageType(val layout: Int) {
        TEXT_IN(R.layout.chat_message_in),
        TEXT_OUT(R.layout.chat_message_out),
        SHARE_IN(R.layout.chat_message_in),
        SHARE_OUT(R.layout.chat_message_out)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ChatMessageViewHolderBase<*> {
        parent ?: throw IllegalArgumentException("Parent view group is null")

        val viewTypeObj = MessageType.values()[viewType];
        val layout = LayoutInflater.from(parent.context)
                .inflate(viewTypeObj.layout, parent, false);
        return when (viewType) {
            MessageType.SHARE_IN.ordinal -> ShareChatMessageViewHolder(layout)
            MessageType.SHARE_OUT.ordinal -> ShareChatMessageViewHolder(layout)
            else -> TextChatMessageViewHolder(layout)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages.getOrNull(position) ?: return -1;
        if (message.direction == ChatMessage.Direction.INCOMING) {
            return when (message.messagePayload) {
                is MessagePayload.ShareMessage -> MessageType.SHARE_IN.ordinal
                else -> MessageType.TEXT_IN.ordinal
            }
        } else {
            return when (message.messagePayload) {
                is MessagePayload.ShareMessage -> MessageType.SHARE_OUT.ordinal
                else -> MessageType.TEXT_OUT.ordinal
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ChatMessageViewHolderBase<*>?, position: Int) {
        val item = getItemAtPosition(position) ?: return;
        holder?.bindTo(item, getItemAtPosition(position.dec())?.direction != item.direction)
    }

    fun getItemAtPosition(position: Int): ChatMessage? = messages.getOrNull(position)

}

