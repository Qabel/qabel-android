package de.qabel.qabelbox.interactor

import de.qabel.core.config.Contact
import de.qabel.qabelbox.dto.ChatMessage
import rx.Observable
import rx.lang.kotlin.observable
import rx.lang.kotlin.toSingletonObservable

open class MockChatUseCase(val chatMessage: ChatMessage,
                      override val contact: Contact,
                      var messages: List<ChatMessage>): ChatUseCase {
    override fun send(text: String): Observable<ChatMessage> {
        messages = listOf(chatMessage)
        return chatMessage.toSingletonObservable()
    }

    override fun retrieve() = observable<ChatMessage> {
        messages.forEach { msg ->
            it.onNext(msg)
        }
        it.onCompleted()
    }
}
