package de.qabel.qabelbox.contacts.view.adapters

import android.view.View
import android.widget.Button
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.extensions.contactColors
import de.qabel.qabelbox.contacts.extensions.initials
import de.qabel.qabelbox.contacts.extensions.readableKey
import de.qabel.qabelbox.contacts.extensions.readableUrl
import de.qabel.qabelbox.contacts.view.widgets.ContactIconDrawable
import kotlinx.android.synthetic.main.fragment_contact_details.view.*
import org.jetbrains.anko.layoutInflater


class ContactDetailsAdapter(private val onSendMessageClick: (identity: Identity) -> Unit) {

    var view: View? = null

    private var currentContact: ContactDto? = null

    fun loadContact(contact: ContactDto) {
        currentContact = contact;
        view?.apply {
            contact_icon_border.background = ContactIconDrawable(contact.contactColors(view!!.context))
            tv_initial.text = contact.initials();
            editTextContactName.text = contact.contact.alias;
            editTextContactDropURL.text = contact.readableUrl();
            editTextContactPublicKey.text = contact.readableKey();

            contact_details_actions.removeAllViews();
            if (contact.identities.size > 1) {
                contact.identities.forEach {
                    addMessageButton(view!!.context.getString(R.string.send_message_as, it.alias));
                }
            } else if (contact.identities.size == 1) {
                addMessageButton(view!!.context.getString(R.string.send_message, contact.identities.first().alias))
            }
        }
    }

    private fun addMessageButton(text: String?) {
        view?.apply {
            val button = view!!.context.layoutInflater.inflate(R.layout.contact_details_message_button, null) as Button;
            button.text = text;
            button.setOnClickListener { button ->
                if (currentContact != null) {
                    contact_details_actions?.indexOfChild(button)?.apply {
                        onSendMessageClick(currentContact!!.identities[this])
                    };
                }
            }
            contact_details_actions?.addView(button);
        }
    }

}
