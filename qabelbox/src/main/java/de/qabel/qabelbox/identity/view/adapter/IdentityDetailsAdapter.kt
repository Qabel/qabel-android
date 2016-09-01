package de.qabel.qabelbox.identity.view.adapter

import android.view.View
import de.qabel.core.config.Identity
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
            identity_icon.background = IdentityIconDrawable(
                    text = identity.initials(),
                    color = identity.color(context))
            identity_initial.text = identity.initials()
            editIdentityAlias.text = identity.alias
            editIdentityEmail.text = identity.email
            editIdentityPhone.text = identity.phone
            detailsIdentityDropURLs.text = identity.readableUrl()
            detailsIdentityPublicKey.text = identity.readableKey()
        }
    }
}
