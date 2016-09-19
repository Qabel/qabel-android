package de.qabel.qabelbox.identity.view.adapter

import android.view.View
import de.qabel.core.config.Identity
import de.qabel.core.config.VerificationStatus
import de.qabel.core.index.formatPhoneNumberReadable
import de.qabel.core.index.isValidPhoneNumber
import de.qabel.core.ui.initials
import de.qabel.core.ui.readableKey
import de.qabel.core.ui.readableUrl
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.extensions.color
import de.qabel.qabelbox.contacts.view.widgets.IdentityIconDrawable
import de.qabel.qabelbox.ui.views.EditTextFont
import de.qabel.qabelbox.ui.views.TextViewFont
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
            setVerifiedDrawables(edit_email, R.drawable.email, identity, identity.emailStatus)
            edit_phone.text = phone
            setVerifiedDrawables(edit_phone, R.drawable.phone, identity, identity.phoneStatus)
            details_drop_urls.text = identity.readableUrl()
            details_pub_key.text = identity.readableKey()
        }
    }

    private fun setVerifiedDrawables(textView: TextViewFont, drawableStart: Int, identity: Identity,
                                     drawableEndStatus: VerificationStatus) =
            textView.setCompoundDrawablesWithIntrinsicBounds(drawableStart, 0,
                    when (drawableEndStatus) {
                        VerificationStatus.NONE -> R.drawable.pencil_grey
                        VerificationStatus.VERIFIED -> R.drawable.earth
                        VerificationStatus.NOT_VERIFIED -> if (identity.isUploadEnabled)
                            R.drawable.sync else R.drawable.pencil
                    }, 0)
}
