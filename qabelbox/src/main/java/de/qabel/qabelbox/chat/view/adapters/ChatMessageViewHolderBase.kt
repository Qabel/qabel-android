package de.qabel.qabelbox.chat.view.adapters

import android.annotation.TargetApi
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.view.View
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.core.ui.initials
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.contacts.extensions.color
import de.qabel.qabelbox.contacts.view.widgets.IdentityIconDrawable
import de.qabel.qabelbox.helper.Formatter
import kotlinx.android.synthetic.main.chat_message_in.view.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.doFromSdk

abstract class ChatMessageViewHolderBase<in T : MessagePayloadDto>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bindTo(message: ChatMessage, showBeginIndicator: Boolean) {
        with(itemView) {
            val incoming = message.direction == ChatDropMessage.Direction.INCOMING
            chat_begin_indicator?.visibility =
                    if (incoming) View.GONE
                    else if (showBeginIndicator) View.VISIBLE
                    else View.INVISIBLE

            setPadding(0, if (showBeginIndicator) 10 else 0, 0, 0)

            tvDate?.text = Formatter.formatDateTimeString(message.time.time, itemView.context)

            if (incoming) {
                setBackgroundColor(itemView, message)
                val contact = message.contact
                contact_avatar.background = if (showBeginIndicator) IdentityIconDrawable(
                        color = contact.color(itemView.context),
                        text = contact.initials(),
                        width = dip(50),
                        height = dip(50)) else null
            }

            @Suppress("UNCHECKED_CAST")
            bindTo(message.messagePayload as T, message)
        }
    }

    abstract fun bindTo(payload: T, message: ChatMessage)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setBackgroundColor(itemView: View, message: ChatMessage) =
            doFromSdk(Build.VERSION_CODES.LOLLIPOP) {
                (itemView.msg_container.background.mutate() as GradientDrawable).color =
                        ColorStateList.valueOf(message.contact.color(itemView.context))
            }

}

