package de.qabel.qabelbox.ui.presenters

import de.qabel.qabelbox.interactor.ChatUseCase
import de.qabel.qabelbox.ui.views.ChatView
import rx.lang.kotlin.onError
import javax.inject.Inject

class MainChatPresenter @Inject constructor(private val view: ChatView,
                                            private val useCase: ChatUseCase) : ChatPresenter {
    override val title: String
        get() = useCase.contact.alias

    init {
        refreshMessages()
    }

    override fun refreshMessages() {
        useCase.retrieve().toList().onError {
            view.showEmpty()
        }.subscribe({ messages ->
            view.refresh()
            if (messages.size > 0) {
                view.showMessages(messages)
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

}
