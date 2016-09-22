package de.qabel.qabelbox.identity.view.presenter

import de.qabel.core.config.Identity


interface IdentityDetailsPresenter {

    var identity: Identity?

    fun loadIdentity()

    fun onSaveAlias(newAlias : String)

    fun onSavePhoneNumber(phoneNumber : String)

    fun onSaveEmail(email : String)

    fun onShowQRClick()

    fun onPrivateControlChanged(checked: Boolean)

}
