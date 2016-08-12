package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.core.config.Contact.ContactStatus
import de.qabel.core.config.Identities
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase
import de.qabel.qabelbox.contacts.view.views.ContactEditView
import de.qabel.qabelbox.navigation.Navigator
import org.jetbrains.anko.AnkoLogger
import javax.inject.Inject

class MainContactEditPresenter @Inject constructor(private val view: ContactEditView,
                                                   private val useCase: ContactsUseCase,
                                                   private val navigator: Navigator) : ContactEditPresenter, AnkoLogger {

    override val title: String
        get() = contact.let {
            if (it.contact.status == ContactStatus.UNKNOWN) view.getNewLabel()
            else view.getEditLabel()
        }

    lateinit var contact: ContactDto
    lateinit var identities: Identities
    override fun loadContact() {
        useCase.loadContactAndIdentities(view.contactKeyId).subscribe { data ->
            this.contact = data.first
            this.identities = data.second
            view.loadContact(data.first, data.second)
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
        contact.identities = identities.entities.filter { identityIds.contains(it.id) }
        useCase.saveContact(contact).subscribe({
            view.showContactSavedToast()
            navigator.popBackStack()
        }, {
            view.showSaveFailed()
        })
    }

}
