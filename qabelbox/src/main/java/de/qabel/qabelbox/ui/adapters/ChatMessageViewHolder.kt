package de.qabel.qabelbox.ui.adapters

import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import de.qabel.qabelbox.R
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.dto.MessagePayload
import kotlinx.android.synthetic.main.chat_message_in.view.*

open class ChatMessageViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {

    open fun bindTo(message: ChatMessage, showBeginIndicator: Boolean = true) {
        var showShare = false;
        when (message.messagePayload) {
            is MessagePayload.TextMessage -> itemView?.tvText?.text = message.messagePayload.message
            is MessagePayload.ShareMessage -> {
                itemView?.shareText?.text = message.messagePayload.message

                val shareStatusLabel = (if (message.direction == ChatMessage.Direction.INCOMING) {
                    when (message.messagePayload.status) {
                        MessagePayload.ShareMessage.ShareStatus.NEW -> R.string.accept_share
                        MessagePayload.ShareMessage.ShareStatus.ACCEPTED -> R.string.open
                        else -> R.string.currently_not_available
                    }
                } else R.string.open);

                itemView?.tvLink?.text = itemView.context.getString(shareStatusLabel)
                showShare = true;
            }
            is MessagePayload.NoMessage -> {
            }
        }
        (itemView?.tvDate as TextView).text = DateUtils.getRelativeTimeSpanString(message.time.time)

        itemView?.messageFileContainer?.visibility = if (showShare) View.VISIBLE else View.GONE;
        itemView?.tvText?.visibility = if (!showShare) View.VISIBLE else View.GONE;
        itemView?.chat_begin_indicator?.visibility = if (showBeginIndicator) View.VISIBLE else View.INVISIBLE;
        itemView?.setPadding(0, if (showBeginIndicator) 10 else 0, 0, 0);
        itemView?.requestLayout();
    }

}

