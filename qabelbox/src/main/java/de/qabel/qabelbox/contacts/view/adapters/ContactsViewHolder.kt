package de.qabel.qabelbox.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
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
            alpha = if (contact.active && contact.identities.size > 0) 1f else 0.35f

            textViewItemName.text = contact.contact.alias
            //TODO Field for Mail/Phone
            //itemView?.textViewItemDetail?.text = contact.contact.email

            tv_initial.text = contact.initials();
            contact_icon_border.background = ContactIconDrawable(
                    contact.contactColors(itemView.context));

            setOnClickListener({ clickListener.invoke(contact) });
            setOnLongClickListener({ longClickListener.invoke(contact); });
        }
    }

    class ContactHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindTo(c: Char) {
            itemView?.txtHeader?.text = c.toString()
        }
    }

}

