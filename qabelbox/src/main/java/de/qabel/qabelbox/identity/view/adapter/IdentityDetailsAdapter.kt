package de.qabel.qabelbox.identity.view.adapter

import android.view.View
import de.qabel.core.config.Identity
import de.qabel.core.index.formatPhoneNumberReadable
import de.qabel.core.index.isValidPhoneNumber
import de.qabel.core.ui.initials
import de.qabel.core.ui.readableKey
import de.qabel.core.ui.readableUrl
import de.qabel.qabelbox.contacts.extensions.color
import de.qabel.qabelbox.contacts.view.widgets.IdentityIconDrawable
import kotlinx.android.synthetic.main.fragment_identity_details.view.*


class IdentityDetailsAdapter() {

    var view: View? = null

    fun loadIdentity(identity: Identity) {
        view?.apply {
            val mail = identity.email ?: ""
            val phone = if (isValidPhoneNumber(identity.phone))
                formatPhoneNumberReadable(identity.phone) else ""

            identity_icon.background = IdentityIconDrawable(
                    text = identity.initials(),
                    color = identity.color(context))
            identity_initial.text = identity.initials()
            edit_alias.text = identity.alias
            edit_email.text = mail
            edit_phone.text = phone
            details_drop_urls.text = identity.readableUrl()
            details_pub_key.text = identity.readableKey()
        }
    }
}
