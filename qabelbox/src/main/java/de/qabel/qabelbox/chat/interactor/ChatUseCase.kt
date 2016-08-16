package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Contact
import de.qabel.qabelbox.chat.dto.ChatMessage
import rx.Observable

interface ChatUseCase {

    val contact: Contact

    fun retrieve(): Observable<ChatMessage>
    fun send(text: String): Observable<ChatMessage>

    fun ignoreContact(): Observable<Unit>
    fun addContact(): Observable<Unit>
}
