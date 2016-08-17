package de.qabel.qabelbox.chat.view.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.contacts.extensions.displayName
import de.qabel.qabelbox.contacts.extensions.initials
import de.qabel.qabelbox.helper.Formatter
import kotlinx.android.synthetic.main.item_chatoverview.view.*

class ConversationViewHolder(val view: View?, val clickListener: (ChatMessage) -> Unit,
                             val longClickListener: (ChatMessage) -> Boolean) : RecyclerView.ViewHolder(view) {

    fun bindTo(message: ChatMessage) {
        view?.apply {
            tv_initial.text = message.contact.initials()
            textViewItemName.text = message.contact.displayName()
            textViewItemMsg.text = message.messagePayload.toMessage()
            tvDate.text = Formatter.formatDateTimeString(message.time.time)
            setOnClickListener({ clickListener.invoke(message) })
            setOnLongClickListener({ longClickListener.invoke(message); })
        }
    }

}
