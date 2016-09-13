package de.qabel.qabelbox.identity.view.presenter

import com.google.i18n.phonenumbers.NumberParseException
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.helper.Formatter
import de.qabel.qabelbox.helper.formatPhoneNumber
import de.qabel.qabelbox.identity.interactor.IdentityUseCase
import de.qabel.qabelbox.identity.view.IdentityDetailsView
import de.qabel.qabelbox.navigation.Navigator
import javax.inject.Inject

class MainIdentityDetailsPresenter @Inject constructor(private val view: IdentityDetailsView,
                                                       private val identityUseCase: IdentityUseCase,
                                                       private val navigator: Navigator) : IdentityDetailsPresenter {

    override var identity: Identity? = null

    override fun loadIdentity() {
        identityUseCase.getIdentity(view.identityKeyId).subscribe({
            identity = it
            view.loadIdentity(it)
        }, {
            view.showDefaultError(it)
        })
    }

    private fun saveIdentity(identity: Identity) {
        identityUseCase.saveIdentity(identity).subscribe({
            view.showIdentitySavedToast()
            view.loadIdentity(identity)
        }, {
            it.printStackTrace()
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
                try {
                    val formatted = formatPhoneNumber(phoneNumber)
                    it.phone = formatted
                } catch (ex: NumberParseException) {
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
