package de.qabel.qabelbox.chat.view.adapters

import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.view.View
import de.qabel.core.ui.displayName
import de.qabel.core.ui.initials
import de.qabel.qabelbox.chat.dto.ChatConversationDto
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.contacts.extensions.color
import de.qabel.qabelbox.contacts.view.widgets.IdentityIconDrawable
import de.qabel.qabelbox.helper.Formatter
import kotlinx.android.synthetic.main.badge.view.*
import kotlinx.android.synthetic.main.item_chatoverview.view.*
import org.jetbrains.anko.dip

class ConversationViewHolder(val view: View?, val clickListener: (ChatMessage) -> Unit,
                             val longClickListener: (ChatMessage) -> Boolean) : RecyclerView.ViewHolder(view) {

    fun bindTo(conversation: ChatConversationDto) {
        view?.apply {
            val fontWeight = if (conversation.newMsgCount > 0) Typeface.BOLD else Typeface.NORMAL
            val contact = conversation.message.contact
            contact_icon.background = IdentityIconDrawable(text = contact.initials(),
                    width = dip(50),
                    height = dip(50),
                    color = contact.color(context))
            textViewItemName.apply {
                text = contact.displayName()
                setTypeface(null, fontWeight)
            }
            textViewItemMsg.apply {
                text = conversation.message.messagePayload.toMessage()
                setTypeface(null, fontWeight)
            }
            badge_text.visibility = if (conversation.newMsgCount > 0) View.VISIBLE else View.INVISIBLE
            badge_text.text = conversation.newMsgCount.toString()

            tvDate.text = Formatter.formatDateTimeString(conversation.message.time.time)
            setOnClickListener({ clickListener.invoke(conversation.message) })
            setOnLongClickListener({ longClickListener.invoke(conversation.message); })
        }
    }

}
