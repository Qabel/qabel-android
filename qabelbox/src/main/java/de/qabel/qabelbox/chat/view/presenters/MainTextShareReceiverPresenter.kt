package de.qabel.qabelbox.chat.view.presenters

import de.qabel.chat.service.ChatService
import de.qabel.qabelbox.chat.view.views.TextShareReceiver
import de.qabel.qabelbox.contacts.dto.EntitySelection
import de.qabel.qabelbox.contacts.interactor.ReadOnlyContactsInteractor
import de.qabel.qabelbox.identity.interactor.ReadOnlyIdentityInteractor
import javax.inject.Inject

class MainTextShareReceiverPresenter @Inject constructor(
        val view: TextShareReceiver,
        val identityInteractor: ReadOnlyIdentityInteractor,
        val contactsInteractor: ReadOnlyContactsInteractor,
        val chatService: ChatService)
: TextShareReceiverPresenter {

    override fun confirm() {
        view.identity?.let { identity ->
            val from = identityInteractor.getIdentity(identity.keyId)
            view.contact?.let { contact ->
                val to = contactsInteractor.findContacts(identity.keyId)
                        .getByKeyIdentifier(contact.keyId)
                if (to == null) {
                    view.showError()
                    return
                }
                chatService.sendTextMessage(view.text, from, to).subscribe(
                        {}, {view.showError()}, {view.stop()})
            } ?: view.showError()
        } ?: view.showError()
    }

    override val availableIdentities: List<EntitySelection>
            = identityInteractor.getIdentities().
            identities.map(::EntitySelection).sortedBy { it.alias }

    override val contacts: List<EntitySelection>
        get() = view.identity?.let {
            contactsInteractor.findContacts(it.keyId).contacts
                    .map(::EntitySelection).sortedBy { it.alias }
        } ?: listOf()

}

