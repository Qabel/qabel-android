package de.qabel.qabelbox.ui.adapters

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.View
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.view.widgets.ContactIconDrawable
import kotlinx.android.synthetic.main.item_contacts.view.*
import kotlinx.android.synthetic.main.list_header.view.*

open class ContactsViewHolder(itemView: View, val clickListener: (ContactDto) -> Unit,
                              val longClickListener: (ContactDto) -> Boolean) :
        RecyclerView.ViewHolder(itemView) {

    open fun bindTo(contact: ContactDto) {
        //TODO EXAMPLE COLORS!
        var colors = mutableListOf<Int>();
        for (i in 0 until contact.identities.size) {
            colors.add(when(contact.identities[i].id % 5){
                0 -> Color.RED
                1 -> Color.BLUE
                2 -> Color.GREEN
                3 -> Color.MAGENTA
                else -> Color.CYAN
            })
        }
        //set grey if no identity associated
        itemView?.alpha = if (contact.identities.size > 0) 1f else 0.35f

        itemView?.textViewItemName?.text = contact.contact.alias
        itemView?.textViewItemDetail?.text = contact.contact.keyIdentifier
        itemView?.tv_initial?.text = getInitials(contact.contact.alias);
        itemView?.contact_icon_border?.background = ContactIconDrawable(colors)
        itemView?.setOnClickListener({ clickListener.invoke(contact) });
        itemView?.setOnLongClickListener({ longClickListener.invoke(contact); });
    }

    private fun getInitials(name: String): String {
        val names = name.split(" ".toRegex());
        var result = StringBuilder();
        (0 until names.size).map {
            result.append(names[it].first().toUpperCase());
        }
        return result.toString();
    }

    class ContactHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bindTo(c: Char) {
            itemView?.txtHeader?.text = c.toString()
        }
    }

}

