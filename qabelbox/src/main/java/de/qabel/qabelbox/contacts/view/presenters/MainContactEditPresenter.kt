package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.core.config.Contact.ContactStatus
import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase
import de.qabel.qabelbox.contacts.view.views.ContactEditView
import de.qabel.qabelbox.identity.interactor.IdentityUseCase
import de.qabel.qabelbox.navigation.Navigator
import org.jetbrains.anko.AnkoLogger
import javax.inject.Inject

class MainContactEditPresenter @Inject constructor(private val view: ContactEditView,
                                                   private val useCase: ContactsUseCase,
                                                   private val identityUseCase: IdentityUseCase,
                                                   private val navigator: Navigator) : ContactEditPresenter, AnkoLogger {

    override val title: String
        get() = contact.let {
            if (it.contact.status == ContactStatus.UNKNOWN) view.getNewLabel()
            else view.getEditLabel()
        }

    lateinit var contact: ContactDto
    lateinit var identities: Identities
    override fun loadContact() {
        contact = view.contactDto
        identityUseCase.getIdentities().subscribe {
            identities = it
            view.loadContact(contact, identities)
        }
    }

    override fun onSaveClick() {
        val nickName = view.getCurrentNick()
        val identityIds = view.getCurrentIdentityIds()
        if (nickName.isEmpty()) {
            view.showEnterNameToast()
            return
        }

        contact.contact.nickName = nickName
        contact.contact.isIgnored = view.isContactIgnored()
        contact.identities = identities.entities.filter { identityIds.contains(it.id) }
        useCase.saveContact(contact).subscribe({
            view.showContactSavedToast()
            navigator.popBackStack()
        }, {
            view.showSaveFailed()
        })
    }

}
