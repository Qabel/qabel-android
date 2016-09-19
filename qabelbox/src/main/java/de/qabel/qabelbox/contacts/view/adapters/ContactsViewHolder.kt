package de.qabel.qabelbox.contacts.view.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import de.qabel.core.index.formatPhoneNumberReadable
import de.qabel.core.index.isValidPhoneNumber
import de.qabel.core.ui.displayName
import de.qabel.core.ui.initials
import de.qabel.core.ui.readableKeyShort
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.extensions.*
import de.qabel.qabelbox.contacts.view.widgets.ContactIconDrawable
import de.qabel.qabelbox.contacts.view.widgets.IdentityIconDrawable
import kotlinx.android.synthetic.main.item_contacts.view.*
import kotlinx.android.synthetic.main.list_header.view.*
import org.jetbrains.anko.dip

class ContactsViewHolder(itemView: View, val clickListener: (ContactDto) -> Unit,
                         val longClickListener: (ContactDto) -> Boolean) :
        RecyclerView.ViewHolder(itemView) {

    fun bindTo(contactDto: ContactDto) {
        //set grey if no identity or assciated identities not active associated
        itemView?.apply {
            val contact = contactDto.contact
            alpha = if (contactDto.active && !contact.isIgnored && contactDto.identities.size > 0)
                1f else 0.5f

            val displayName = contact.displayName()
            textViewItemName.text = displayName
            if (!displayName.equals(contact.alias)) {
                textViewItemAlias.text = contact.alias
                textViewItemAlias.visibility = View.VISIBLE
            } else {
                textViewItemAlias.visibility = View.GONE
            }

            val mail = contact.email ?: ""
            val phone = if (isValidPhoneNumber(contact.phone))
                formatPhoneNumberReadable(contact.phone) else ""
            setOrGone(textViewItemMail, mail)
            setOrGone(textViewItemPhone, phone)

            if (contact.email.isNullOrBlank() && contact.phone.isNullOrBlank()) {
                text_public_key.text = contact.readableKeyShort()
                text_public_key.visibility = View.VISIBLE
            } else {
                text_public_key.visibility = View.GONE
            }


            contact_inititals.background = IdentityIconDrawable(
                    color = contact.color(itemView.context),
                    text = contact.initials(),
                    width = dip(45),
                    height = dip(45),
                    fontSize = dip(27),
                    isBold = true)
            contact_icon_border.background = ContactIconDrawable(contactDto.contactColors(itemView.context))

            setOnClickListener({ clickListener.invoke(contactDto) })
            setOnLongClickListener({ longClickListener.invoke(contactDto); })
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

