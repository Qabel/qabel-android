package de.qabel.qabelbox.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import de.qabel.qabelbox.R
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.dto.MessagePayload

open class ChatMessageAdapter(var messages: List<ChatMessage>): RecyclerView.Adapter<ChatMessageViewHolder>() {

    companion object {
        const val NO_MESSAGE = R.layout.item_no_message
        const val INCOMING_TEXT = R.layout.item_chat_message_in
        const val INCOMING_SHARE = R.layout.item_share_message_in
        const val OUTGOING_TEXT = R.layout.item_chat_message_out
        const val OUTGOING_SHARE = R.layout.item_share_message_out
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ChatMessageViewHolder? {
        parent ?: throw IllegalArgumentException("Parent view group is null")

        val layout = LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false);
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
        holder?.bindTo(getItemAtPosition(position) ?: return)
    }

    fun getItemAtPosition(position: Int): ChatMessage? = messages.getOrNull(position)

}
