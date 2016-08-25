package de.qabel.qabelbox.contacts.view.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import de.qabel.core.ui.displayName
import de.qabel.core.ui.initials
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.extensions.*
import de.qabel.qabelbox.contacts.view.widgets.ContactIconDrawable
import kotlinx.android.synthetic.main.item_contacts.view.*
import kotlinx.android.synthetic.main.list_header.view.*

class ContactsViewHolder(itemView: View, val clickListener: (ContactDto) -> Unit,
                         val longClickListener: (ContactDto) -> Boolean) :
        RecyclerView.ViewHolder(itemView) {

    fun bindTo(contact: ContactDto) {
        //set grey if no identity or assciated identities not active associated
        itemView?.apply {
            alpha = if (contact.active && !contact.contact.isIgnored && contact.identities.size > 0)
                1f else 0.5f

            val displayName = contact.contact.displayName()
            textViewItemName.text = displayName
            if (!displayName.equals(contact.contact.alias)) {
                textViewItemAlias.text = contact.contact.alias
                textViewItemAlias.visibility = View.VISIBLE
            } else {
                textViewItemAlias.visibility = View.GONE
            }

            setOrGone(textViewItemMail, contact.contact.email)
            setOrGone(textViewItemPhone, contact.contact.phone)

            tv_initial.text = contact.contact.initials();
            contact_icon_border.background = ContactIconDrawable(
                    contact.contactColors(itemView.context));

            setOnClickListener({ clickListener.invoke(contact) });
            setOnLongClickListener({ longClickListener.invoke(contact); });
        }
    }

    private fun setOrGone(view: TextView, text: String?) {
        if (text != null && !text.isEmpty()) {
            view.text = text
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }

    class ContactHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindTo(c: Char) {
            itemView.txtHeader.text = c.toString()
        }
    }

}

