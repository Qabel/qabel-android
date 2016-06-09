package de.qabel.qabelbox.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import de.qabel.qabelbox.R
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.dto.MessagePayload
import de.qabel.qabelbox.ui.adapters.ChatMessageViewHolder

open class ChatMessageAdapter(var messages: List<ChatMessage>): RecyclerView.Adapter<ChatMessageViewHolder>() {

    companion object {
        const val NO_MESSAGE = R.layout.item_no_message
        const val INCOMING_TEXT = R.layout.item_chat_message_in
        const val INCOMING_SHARE = R.layout.item_share_message_in
        const val OUTGOING_TEXT = R.layout.item_chat_message_out
        const val OUTGOING_SHARE = R.layout.item_share_message_out
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ChatMessageViewHolder? {
        if (parent == null) {
            throw IllegalArgumentException("Parent view group is null")
        }

        val layout = when(viewType) {
            0 -> LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_message_in, parent, false);
            else -> LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_message_in, parent, false);
        }
        return ChatMessageViewHolder(layout)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages.getOrNull(position) ?: return NO_MESSAGE
        return when(message.messagePayload) {
            is MessagePayload.NoMessage -> NO_MESSAGE
            is MessagePayload.TextMessage -> when(message.direction) {
                ChatMessage.Direction.INCOMING -> INCOMING_TEXT
                ChatMessage.Direction.OUTGOING -> OUTGOING_TEXT
            }
            is MessagePayload.ShareMessage -> when(message.direction) {
                ChatMessage.Direction.INCOMING -> INCOMING_SHARE
                ChatMessage.Direction.OUTGOING -> OUTGOING_SHARE
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ChatMessageViewHolder?, position: Int) {
        throw UnsupportedOperationException()
    }

    fun getItemAtPosition(position: Int): ChatMessage? = messages.getOrNull(position)

}

