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
        if(view != null) {
            view!!.contact_icon_border?.background = ContactIconDrawable(contact.contactColors(view!!.ctx))
            view?.tv_initial?.text = contact.initials();
            view?.editTextContactName?.text = contact.contact.alias;
            view?.editTextContactDropURL?.text = contact.readableUrl();
            view?.editTextContactPublicKey?.text = contact.readableKey();

            view?.contact_details_actions?.removeAllViews();
            if (contact.identities.size > 0) {
                contact.identities.forEach { identity ->
                    addMessageButton(view?.ctx?.getString(R.string.send_message_as, identity.alias));
                }
            } else {
                addMessageButton(view?.ctx?.getString(R.string.btn_chat_send, contact.identities.first().alias))
            }
        }
    }

    private fun addMessageButton(text: String?) {
        val button = view?.ctx?.layoutInflater?.inflate(R.layout.contact_details_message_button, null) as Button;
        button.text = text;
        button.setOnClickListener { button ->
            if (currentContact != null) {
                val index = view?.contact_details_actions?.indexOfChild(button);
                if (index!! >= 0) {
                    onSendMessageClick(currentContact!!.identities[index])
                }
            }
        }
        view?.contact_details_actions?.addView(button);
    }

}
