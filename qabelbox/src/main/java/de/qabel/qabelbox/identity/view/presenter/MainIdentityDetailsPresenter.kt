package de.qabel.qabelbox.identity.view.presenter

import de.qabel.core.config.Identity
import de.qabel.core.index.formatPhoneNumber
import de.qabel.core.index.isValidPhoneNumber
import de.qabel.core.logging.QabelLog
import de.qabel.core.logging.error
import de.qabel.qabelbox.helper.Formatter
import de.qabel.qabelbox.identity.interactor.IdentityUseCase
import de.qabel.qabelbox.identity.view.IdentityDetailsView
import de.qabel.qabelbox.navigation.Navigator
import javax.inject.Inject

class MainIdentityDetailsPresenter @Inject constructor(private val view: IdentityDetailsView,
                                                       private val identityUseCase: IdentityUseCase,
                                                       private val navigator: Navigator) : IdentityDetailsPresenter, QabelLog {

    override var identity: Identity? = null

    override fun loadIdentity() {
        identityUseCase.getIdentity(view.identityKeyId).subscribe({
            identity = it
            view.loadIdentity(it)
        }, {
            error("Error loading identity ${view.identityKeyId} ", it)
            view.showDefaultError(it)
        })
    }

    private fun saveIdentity(identity: Identity) {
        identityUseCase.saveIdentity(identity).subscribe({
            view.showIdentitySavedToast()
            view.loadIdentity(identity)
        }, {
            error("Saving identity failed!", it)
            view.showSaveFailed()
        })
    }

    override fun onSaveAlias(newAlias: String) {
        identity?.let {
            if (newAlias.isEmpty()) {
                view.showAliasEmptyInvalid()
                view.showEnterAliasDialog("")
            } else {
                it.alias = newAlias
                saveIdentity(it)
            }
        }
    }

    override fun onSavePhoneNumber(phoneNumber: String) {
        identity?.let {
            if (!phoneNumber.isEmpty()) {
                if (isValidPhoneNumber(phoneNumber)) {
                    it.phone = formatPhoneNumber(phoneNumber)
                } else {
                    view.showPhoneInvalid()
                    view.showEnterPhoneDialog(phoneNumber)
                }
            } else {
                it.phone = phoneNumber
            }
            saveIdentity(it)
        }
    }

    override fun onSaveEmail(email: String) {
        identity?.let {
            if (!email.isEmpty() && !Formatter.isEMailValid(email)) {
                view.showEmailInvalid()
                view.showEnterEmailDialog(email)
                return
            } else {
                it.email = email
                saveIdentity(it)
            }
        }
    }

    override fun onShowQRClick() {
        identity?.let {
            navigator.selectQrCodeFragment(it.toContact())
        }
    }
}
