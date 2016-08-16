package de.qabel.qabelbox.chat.view.presenters

import de.qabel.core.config.Contact
import de.qabel.qabelbox.chat.interactor.ChatUseCase
import de.qabel.qabelbox.chat.view.views.ChatView
import de.qabel.qabelbox.contacts.extensions.displayName
import de.qabel.qabelbox.navigation.Navigator
import rx.lang.kotlin.onError
import javax.inject.Inject

class MainChatPresenter @Inject constructor(private val view: ChatView,
                                            private val useCase: ChatUseCase,
                                            private val navigator: Navigator) : ChatPresenter {
    override val title: String
        get() = useCase.contact.displayName()

    override val showContactMenu: Boolean
        get() = useCase.contact.status == Contact.ContactStatus.UNKNOWN

    override val subtitle: String
        get() = if (!useCase.contact.displayName().equals(useCase.contact.alias))
            useCase.contact.alias else ""

    override fun refreshMessages() {
        useCase.retrieve().toList().onError {
            view.showEmpty()
        }.subscribe({ messages ->
            if (messages.size > 0) {
                view.showMessages(messages)
                view.sendMessageStateChange()
            } else (view.showEmpty())
        })
    }

    override fun sendMessage() {
        if (view.messageText.isNotEmpty()) {
            useCase.send(view.messageText).doOnCompleted {
                refreshMessages()
            }.subscribe { message ->
                view.appendMessage(message)
            }
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

}
