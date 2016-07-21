package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Contact
import de.qabel.qabelbox.chat.dto.ChatMessage
import rx.Observable

interface ChatUseCase {
    fun retrieve(): Observable<ChatMessage>
    fun send(text: String): Observable<ChatMessage>

    val contact: Contact
}
