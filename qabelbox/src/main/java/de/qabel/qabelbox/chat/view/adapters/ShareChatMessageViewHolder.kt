package de.qabel.qabelbox.chat.view.adapters

import android.view.View
import com.squareup.picasso.Picasso
import de.qabel.chat.repository.entities.BoxFileChatShare
import de.qabel.chat.repository.entities.ShareStatus
import de.qabel.qabelbox.R
import de.qabel.qabelbox.box.provider.ShareId
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.ui.extensions.setOrGone
import kotlinx.android.synthetic.main.chat_message_share.view.*
import java.net.URLConnection

open class ShareChatMessageViewHolder(itemView: View, val onClick: (msg: ChatMessage) -> Unit) :
        ChatMessageViewHolderBase<MessagePayloadDto.ShareMessage>(itemView) {

    override fun bindTo(payload: MessagePayloadDto.ShareMessage, chatMsg: ChatMessage) {
        with(itemView) {
            var isPreviewed = false
            if (payload.share.isUnavailable()) {
                val shareStatusLabel =
                        when (payload.share.status) {
                            ShareStatus.UNREACHABLE -> R.string.currently_not_available
                            else -> R.string.permanently_unavailable
                        }
                msg_overlay.text = context.getString(shareStatusLabel)
                msg_overlay.visibility = View.VISIBLE
            } else {
                msg_overlay.visibility = View.GONE

                val shareUri = ShareId.create(payload.share).toUri()
                val mimeType = URLConnection.guessContentTypeFromName(shareUri.toString()) ?: ""
                if (mimeType.startsWith("image")) {
                    Picasso.with(context).load(shareUri).resize(700, 700).onlyScaleDown().centerInside().into(messageFilePreview)
                    isPreviewed = true
                }
            }
            messageFileIcon.visibility = if (isPreviewed) View.GONE else View.VISIBLE
            messageFilePreview.visibility = if (isPreviewed) View.VISIBLE else View.GONE
            file_name.text = payload.share.name
            message.setOrGone(if (payload.message != payload.share.name) payload.message else "")
            setOnClickListener { onClick(chatMsg) }
        }
    }

    private fun BoxFileChatShare.isUnavailable() =
            listOf(ShareStatus.DELETED, ShareStatus.UNREACHABLE, ShareStatus.REVOKED)
                    .contains(status)
}
