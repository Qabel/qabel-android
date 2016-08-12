package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.core.config.Identity
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase
import de.qabel.qabelbox.contacts.view.views.ContactDetailsView
import de.qabel.qabelbox.navigation.Navigator
import org.jetbrains.anko.AnkoLogger
import javax.inject.Inject

class MainContactDetailsPresenter @Inject constructor(private val view: ContactDetailsView,
                                                      private val useCase: ContactsUseCase,
                                                      val navigator: Navigator) : ContactDetailsPresenter, AnkoLogger {
    override val title: String
        get() = contact?.contact?.alias ?: ""

    var contact: ContactDto? = null

    override fun refreshContact() {
        useCase.loadContact(view.contactKeyId).subscribe { contact -> this.contact = contact; view.loadContact(contact); }
    }

    override fun handleEditClick() {
        contact?.let {
            navigator.selectContactEdit(it)
        }
    }

    override fun onSendMsgClick(identity: Identity) {
        contact?.let {
            navigator.selectContactChat(it.contact.keyIdentifier, identity)
        }
    }
}
