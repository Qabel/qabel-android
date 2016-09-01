package de.qabel.qabelbox.identity.view

import de.qabel.core.config.Identity
import de.qabel.qabelbox.ui.QblView

interface IdentityDetailsView : QblView {

    var identityKeyId: String

    fun loadIdentity(identity: Identity)

    fun showEnterNameToast()
    fun showIdentitySavedToast()
    fun showSaveFailed()

}

