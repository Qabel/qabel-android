package de.qabel.qabelbox.ui.adapters

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.View
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.view.ContactIconDrawable
import kotlinx.android.synthetic.main.item_contacts.view.*
import kotlinx.android.synthetic.main.list_header.view.*

open class ContactsViewHolder(itemView: View, val clickListener: (ContactDto) -> Unit,
                              val longClickListener: (ContactDto) -> Boolean) :
        RecyclerView.ViewHolder(itemView) {

    //TODO EXAMPLE COLORS!
    var allColor = listOf(Color.RED, Color.GREEN, Color.BLUE);
    var singleColor =listOf(Color.RED);
    var twoColors = listOf(Color.RED, Color.GREEN);

    open fun bindTo(contact: ContactDto) {
        val color =  when(contact.contact.id % 3){
            0 -> singleColor
            1 -> twoColors
            else -> allColor
        };
        itemView?.textViewItemName?.text = contact.contact.alias
        itemView?.textViewItemDetail?.text = contact.contact.keyIdentifier
        itemView?.tv_initial?.text = getInitials(contact.contact.alias);
        itemView?.contact_icon_border?.background = ContactIconDrawable(color)
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

