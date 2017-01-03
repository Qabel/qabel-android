package de.qabel.qabelbox.chat.view.presenters

import de.qabel.qabelbox.chat.view.views.TextShareReceiver
import de.qabel.qabelbox.contacts.dto.EntitySelection
import de.qabel.qabelbox.contacts.interactor.ReadOnlyContactsInteractor
import de.qabel.qabelbox.identity.interactor.ReadOnlyIdentityInteractor
import javax.inject.Inject

class MainTextShareReceiverPresenter @Inject constructor(
        val view: TextShareReceiver,
        val identityInteractor: ReadOnlyIdentityInteractor,
        val contactsInteractor: ReadOnlyContactsInteractor)
: TextShareReceiverPresenter {

    override fun confirm() {
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

