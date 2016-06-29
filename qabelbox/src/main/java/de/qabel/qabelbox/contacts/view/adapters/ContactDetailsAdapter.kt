package de.qabel.qabelbox.contacts.view.adapters

import android.widget.Button
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.extensions.*
import de.qabel.qabelbox.contacts.view.views.ContactDetailsFragment
import de.qabel.qabelbox.contacts.view.widgets.ContactIconDrawable
import kotlinx.android.synthetic.main.fragment_contact_details.*
import org.jetbrains.anko.ctx
import org.jetbrains.anko.layoutInflater


class ContactDetailsAdapter(private val onSendMessageClick: (identity: Identity) -> Unit) {

    var view: ContactDetailsFragment? = null

    private var currentContact: ContactDto? = null

    fun loadContact(contact: ContactDto) {
        currentContact = contact;
        view?.apply {
            contact_icon_border.background = ContactIconDrawable(contact.contactColors(ctx))
            tv_initial.text = contact.initials();
            editTextContactName.text = contact.contact.alias;
            editTextContactDropURL.text = contact.readableUrl();
            editTextContactPublicKey.text = contact.readableKey();

            contact_details_actions.removeAllViews();
            if (contact.identities.size > 0) {
                contact.identities.forEach { identity ->
                    addMessageButton(ctx.getString(R.string.send_message_as, identity.alias));
                }
            } else {
                addMessageButton(ctx.getString(R.string.btn_chat_send, contact.identities.first().alias))
            }
        }
    }

    private fun addMessageButton(text: String?) {
        view?.apply {
            val button = ctx.layoutInflater.inflate(R.layout.contact_details_message_button, null) as Button;
            button.text = text;
            button.setOnClickListener { button ->
                if (currentContact != null) {
                    val index = contact_details_actions?.indexOfChild(button);
                    if (index!! >= 0) {
                        onSendMessageClick(currentContact!!.identities[index])
                    }
                }
            }
            contact_details_actions?.addView(button);
        }
    }

}
