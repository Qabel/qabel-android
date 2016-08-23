package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Contact
import de.qabel.core.repository.framework.PagingResult
import de.qabel.qabelbox.chat.dto.ChatMessage
import rx.Observable
import rx.Single
import rx.lang.kotlin.toSingletonObservable

open class MockChatUseCase(val chatMessage: ChatMessage,
                           override val contact: Contact,
                           var messages: List<ChatMessage>) : ChatUseCase {

    override fun load(offset: Int, pageSize: Int): Observable<PagingResult<ChatMessage>> {
        return PagingResult(messages.size, messages.filterIndexed { i, chatMessage ->
            i >= offset && i <= (offset + pageSize)
        }).toSingletonObservable()
    }

    override fun send(text: String): Single<ChatMessage> {
        messages = listOf(chatMessage)
        return chatMessage.toSingletonObservable().toSingle()
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
