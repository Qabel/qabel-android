package de.qabel.qabelbox.chat.view.presenters

import de.qabel.qabelbox.chat.interactor.ChatUseCase
import de.qabel.qabelbox.chat.view.views.ChatView
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
