package de.qabel.qabelbox.chat.view.presenters

import de.qabel.chat.repository.entities.ShareStatus
import de.qabel.core.config.Contact
import de.qabel.core.ui.DataViewProxy
import de.qabel.core.ui.displayName
import de.qabel.qabelbox.box.provider.ShareId
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.chat.interactor.ChatUseCase
import de.qabel.qabelbox.chat.view.views.ChatView
import de.qabel.qabelbox.navigation.Navigator
import javax.inject.Inject

class MainChatPresenter @Inject constructor(private val view: ChatView,
                                            private val useCase: ChatUseCase,
                                            private val navigator: Navigator) : ChatPresenter {

    override val proxy = DataViewProxy({ offset, pageSize -> useCase.load(offset, pageSize) }, view)

    override val title: String
        get() = useCase.contact.displayName()

    override val showContactMenu: Boolean
        get() = useCase.contact.status == Contact.ContactStatus.UNKNOWN

    override val subtitle: String
        get() = if (useCase.contact.displayName() != useCase.contact.alias)
            useCase.contact.alias else ""

    override fun refreshMessages() = proxy.load()

    override fun sendMessage() {
        if (view.messageText.isNotEmpty()) {
            useCase.send(view.messageText).subscribe({ message ->
                view.appendData(listOf(message))
                proxy.incRange(1)
            }, { proxy.load() })
            view.messageText = ""
        }
    }

    override fun handleHeaderClick() =
            navigator.selectContactDetailsFragment(useCase.contact)

    override fun handleContactAddClick() {
        useCase.addContact().subscribe({
            view.refreshContactOverlay()
        }, {
            view.showError(it)
        })
    }

    override fun handleContactIgnoreClick() {
        useCase.ignoreContact().subscribe({
            navigator.popBackStack()
        }, {
            view.showError(it)
        })
    }

    override fun handleMsgClick(msg: ChatMessage) {
        if (msg.messagePayload is MessagePayloadDto.ShareMessage) {
            val share = msg.messagePayload.share
            when (share.status) {
                ShareStatus.NEW -> {
                    useCase.acceptShare(msg).subscribe({
                        view.openShare(ShareId.create(share))
                    }, {
                        view.showError(it)
                    })
                }
                ShareStatus.CREATED, ShareStatus.ACCEPTED, ShareStatus.UNREACHABLE ->
                    view.openShare(ShareId.create(share))
            }
        }
    }

}
