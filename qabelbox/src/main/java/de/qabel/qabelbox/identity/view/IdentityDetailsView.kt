package de.qabel.qabelbox.identity.view

import de.qabel.core.config.Identity
import de.qabel.qabelbox.ui.QblView

interface IdentityDetailsView : QblView {

    var identityKeyId: String

    fun loadIdentity(identity: Identity)

    fun showEnterAliasDialog(current: String)
    fun showEnterPhoneDialog(current: String)
    fun showEnterEmailDialog(current: String)

    fun showAliasEmptyInvalid()
    fun showEmailInvalid()
    fun showPhoneInvalid()

    fun showIdentitySavedToast()
    fun showSaveFailed()

}

