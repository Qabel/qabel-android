package de.qabel.qabelbox.chat.view.adapters

import android.view.View
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.chat.repository.entities.ShareStatus
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import kotlinx.android.synthetic.main.chat_message_share.view.*

open class ShareChatMessageViewHolder(itemView: View) :
        ChatMessageViewHolderBase<MessagePayloadDto.ShareMessage>(itemView) {

    override fun bindTo(payload: MessagePayloadDto.ShareMessage, message: ChatMessage) {
        val shareStatusLabel = (if (message.direction == ChatDropMessage.Direction.INCOMING) {
            when (payload.share.status) {
                ShareStatus.NEW -> R.string.accept_share
                ShareStatus.ACCEPTED -> R.string.open
                ShareStatus.UNREACHABLE -> R.string.currently_not_available
                else -> R.string.permanently_unavailable
            }
        } else R.string.open)

        itemView?.tvLink?.text = itemView.context.getString(shareStatusLabel)
        itemView?.shareText?.text = payload.message
    }
}
