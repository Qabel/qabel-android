package de.qabel.qabelbox.chat.view.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.ui.DataViewAdapter

open class ChatMessageAdapter() :
        RecyclerView.Adapter<ChatMessageViewHolderBase<*>>(), DataViewAdapter<ChatMessage> {

    override var data: List<ChatMessage> = emptyList()

    override fun notifyView() = notifyDataSetChanged()
    override fun notifyViewRange(start: Int, count: Int) = notifyItemRangeChanged(start, count)

    enum class MessageType(val layout: Int, val contentLayout: Int) {
        TEXT_IN(R.layout.chat_message_in, R.layout.chat_message_text),
        TEXT_OUT(R.layout.chat_message_out, R.layout.chat_message_text),
        SHARE_IN(R.layout.chat_message_in, R.layout.chat_message_share),
        SHARE_OUT(R.layout.chat_message_out, R.layout.chat_message_share)
    }

    override fun prepend(models: List<ChatMessage>) {
        super.prepend(models.reversed())
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ChatMessageViewHolderBase<*> {
        parent ?: throw IllegalArgumentException("Parent view group is null")

        val viewTypeObj = MessageType.values()[viewType]
        val layout = LayoutInflater.from(parent.context)
                .inflate(viewTypeObj.layout, parent, false)
        val contentLayout = LayoutInflater.from(parent.context)
                .inflate(viewTypeObj.contentLayout, parent, false)

        (layout.findViewById(R.id.chatContent) as LinearLayout).addView(contentLayout)
        return when (viewTypeObj.contentLayout) {
            R.layout.chat_message_share -> ShareChatMessageViewHolder(layout)
            else -> TextChatMessageViewHolder(layout)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = data.getOrNull(position) ?: return -1
        if (message.direction == ChatDropMessage.Direction.INCOMING) {
            return when (message.messagePayload) {
                is MessagePayloadDto.ShareMessage -> MessageType.SHARE_IN.ordinal
                else -> MessageType.TEXT_IN.ordinal
            }
        } else {
            return when (message.messagePayload) {
                is MessagePayloadDto.ShareMessage -> MessageType.SHARE_OUT.ordinal
                else -> MessageType.TEXT_OUT.ordinal
            }
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ChatMessageViewHolderBase<*>?, position: Int) {
        val item = getItemAtPosition(position) ?: return
        holder?.bindTo(item, getItemAtPosition(position.dec())?.direction != item.direction)
    }

    fun getItemAtPosition(position: Int): ChatMessage? = data.getOrNull(position)
}

