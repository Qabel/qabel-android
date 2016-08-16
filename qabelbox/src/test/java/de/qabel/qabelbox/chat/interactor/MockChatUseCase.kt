package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Contact
import de.qabel.qabelbox.chat.dto.ChatMessage
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
    override fun ignoreContact(): Observable<Unit> {
        contact.isIgnored = true
        return Unit.toSingletonObservable()
    }

    override fun addContact(): Observable<Unit> {
        contact.status == Contact.ContactStatus.NORMAL
        return Unit.toSingletonObservable()
    }

}
