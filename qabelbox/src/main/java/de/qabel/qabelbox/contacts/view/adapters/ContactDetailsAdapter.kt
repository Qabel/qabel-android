package de.qabel.qabelbox.contacts.view.adapters

import android.view.View
import android.widget.Button
import de.qabel.core.config.Identity
import de.qabel.core.index.formatPhoneNumberReadable
import de.qabel.core.index.isValidPhoneNumber
import de.qabel.core.ui.displayName
import de.qabel.core.ui.initials
import de.qabel.core.ui.readableKey
import de.qabel.core.ui.readableUrl
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.extensions.*
import de.qabel.qabelbox.contacts.view.widgets.ContactIconDrawable
import de.qabel.qabelbox.contacts.view.widgets.IdentityIconDrawable
import de.qabel.qabelbox.ui.extensions.setOrGone
import kotlinx.android.synthetic.main.fragment_contact_details.view.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.layoutInflater

class ContactDetailsAdapter(private val onSendMessageClick: (identity: Identity) -> Unit) {

    var view: View? = null

    var currentContact: ContactDto? = null

    fun loadContact(contactDto: ContactDto) {
        currentContact = contactDto
        view?.apply {
            val contact = contactDto.contact
            val nickname = contact.displayName()
            contact_icon_border.background = ContactIconDrawable(contactDto.contactColors(context))
            val size = dip(95)
            contact_initials.background = IdentityIconDrawable(text = contact.initials(),
                    width = size, height = size,
                    color = contact.color(context))

            if (!nickname.equals(contact.alias)) {
                text_nick.text = nickname
                text_alias.text = contact.alias
                text_alias.visibility = View.VISIBLE
            } else {
                text_nick.text = nickname
                text_alias.visibility = View.GONE
            }
            text_drop.text = contact.readableUrl()
            text_public_key.text = contact.readableKey()
            val mail = contact.email ?: ""
            val phone = if (isValidPhoneNumber(contact.phone))
                formatPhoneNumberReadable(contact.phone) else ""

            text_phone.setOrGone(phone)
            text_email.setOrGone(mail)

            contact_details_actions.removeAllViews()
            with(contactDto.identities) {
                if (size > 1 || (size == 1 && !contactDto.active)) {
                    forEach {
                        addMessageButton(context.getString(R.string.send_message_as, it.alias))
                    }
                } else if (size == 1) {
                    addMessageButton(context.getString(R.string.send_message))
                }
            }
        }
    }

    private fun addMessageButton(text: String) {
        currentContact?.apply {
            view?.apply {
                val button = context.layoutInflater.inflate(R.layout.contact_details_message_button, null) as Button
                button.text = text
                button.setOnClickListener { button ->
                    if (currentContact != null) {
                        contact_details_actions?.indexOfChild(button)?.apply {
                            onSendMessageClick(identities[this])
                        }
                    }
                }
                contact_details_actions?.addView(button)
            }
        }
    }

}
