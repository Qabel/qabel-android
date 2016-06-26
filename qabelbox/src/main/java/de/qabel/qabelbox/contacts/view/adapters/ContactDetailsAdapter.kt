package de.qabel.qabelbox.contacts.view.adapters

import android.graphics.Color
import android.widget.Button
import android.widget.TextView
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.util.ContactUtil
import de.qabel.qabelbox.contacts.view.views.ContactDetailsFragment
import de.qabel.qabelbox.contacts.view.widgets.ContactIconDrawable
import kotlinx.android.synthetic.main.fragment_contact_details.*
import org.jetbrains.anko.ctx
import org.jetbrains.anko.layoutInflater


class ContactDetailsAdapter(private val onSendMessageClick: (identity: Identity) -> Unit) : ContactUtil() {

    var view: ContactDetailsFragment? = null

    private var currentContact: ContactDto? = null

    fun loadContact(contact: ContactDto) {
        currentContact = contact;
        //TODO EXAMPLE COLORS!
        var colors = mutableListOf<Int>();
        for (i in 0 until contact.identities.size) {
            colors.add(when (contact.identities[i].id % 5) {
                0 -> Color.RED
                1 -> Color.BLUE
                2 -> Color.GREEN
                3 -> Color.MAGENTA
                else -> Color.CYAN
            })
        }
        view?.contact_icon_border?.background = ContactIconDrawable(colors)
        view?.tv_initial?.text = getInitials(contact.contact.alias);
        view?.editTextContactName?.text = contact.contact.alias;
        view?.editTextContactDropURL?.text = getReadableUrl(contact.contact.dropUrls.first().toString());
        view?.editTextContactPublicKey?.setText(getReadableKey(contact.contact.ecPublicKey.readableKeyIdentifier), TextView.BufferType.SPANNABLE);

        view?.contact_details_actions?.removeAllViews();
        if (contact.identities.size > 0) {
            contact.identities.forEach { identity ->
                addMessageButton(view?.ctx?.getString(R.string.send_message_as, identity.alias));
            }
        } else {
            addMessageButton(view?.ctx?.getString(R.string.btn_chat_send, contact.identities.first().alias))
        }
    }

    private fun addMessageButton(text: String?) {
        var button = view?.ctx?.layoutInflater?.inflate(R.layout.contact_details_message_button, null) as Button;
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
