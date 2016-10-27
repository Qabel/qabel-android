package de.qabel.qabelbox.chat.view.adapters

import android.annotation.TargetApi
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RotateDrawable
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.view.View
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.core.ui.initials
import de.qabel.qabelbox.R
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

            chat_begin_indicator.visibility =
                    if (showBeginIndicator) View.VISIBLE else View.INVISIBLE

            setPadding(0, if (showBeginIndicator) 10 else 0, 0, 0)

            tvDate.text = Formatter.formatDateTimeString(message.time.time, context)

            if (incoming) {
                val contact = message.contact
                val contactColor = contact.color(context)
                setBackgroundColor(msg_container.background, contactColor)
                contact_avatar.background = if (showBeginIndicator) IdentityIconDrawable(
                        color = contactColor,
                        text = contact.initials(),
                        width = dip(40),
                        height = dip(40)) else null
                if (showBeginIndicator) {
                    setBackgroundColor(chat_begin_indicator.background, contactColor)
                }
            }

            @Suppress("UNCHECKED_CAST")
            bindTo(message.messagePayload as T, message)
        }
    }

    abstract fun bindTo(payload: T, message: ChatMessage)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setBackgroundColor(drawable: Drawable, color: Int) =
            doFromSdk(Build.VERSION_CODES.LOLLIPOP) {
                val mutateDrawable = drawable.mutate()
                when (mutateDrawable) {
                    is GradientDrawable -> mutateDrawable.setColor(color)
                    is LayerDrawable -> {
                        val targetDrawable = mutateDrawable.findDrawableByLayerId(R.id.target_layer) as RotateDrawable
                        (targetDrawable.drawable as GradientDrawable).setColor(color)
                    }
                    else -> error("Cannot set color on drawable! ${mutateDrawable.javaClass.name}")
                }
            }

}

