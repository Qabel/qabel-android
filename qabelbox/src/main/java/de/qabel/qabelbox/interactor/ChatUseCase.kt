package de.qabel.qabelbox.interactor

import de.qabel.core.config.Contact
import de.qabel.qabelbox.dto.ChatMessage
import rx.Observable

interface ChatUseCase {
    fun retrieve(): Observable<List<ChatMessage>>

    val contact: Contact
}
