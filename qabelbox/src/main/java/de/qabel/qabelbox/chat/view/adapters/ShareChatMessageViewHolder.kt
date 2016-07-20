package de.qabel.qabelbox.chat.view.adapters

import android.view.View
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayload
import kotlinx.android.synthetic.main.chat_message_share.view.*

open class ShareChatMessageViewHolder(itemView: View) :
        ChatMessageViewHolderBase<MessagePayload.ShareMessage>(itemView) {

    override fun bindTo(payload: MessagePayload.ShareMessage, message: ChatMessage) {
        val shareStatusLabel = (if (message.direction == ChatMessage.Direction.INCOMING) {
            when (payload.status) {
                MessagePayload.ShareMessage.ShareStatus.NEW -> R.string.accept_share
                MessagePayload.ShareMessage.ShareStatus.ACCEPTED -> R.string.open
                MessagePayload.ShareMessage.ShareStatus.NOT_REACHABLE -> R.string.currently_not_available
                else -> R.string.permanently_unavailable
            }
        } else R.string.open);

        itemView?.tvLink?.text = itemView.context.getString(shareStatusLabel)
        itemView?.shareText?.text = payload.message
    }
}
