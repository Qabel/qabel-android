package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Contact
import de.qabel.core.repository.framework.PagingResult
import de.qabel.qabelbox.chat.dto.ChatMessage
import rx.Observable
import rx.Single

interface ChatUseCase {

    val contact: Contact

    fun load(offset: Int, pageSize: Int) : Observable<PagingResult<ChatMessage>>

    fun send(text: String): Observable<ChatMessage>

    fun ignoreContact(): Observable<Unit>
    fun addContact(): Observable<Unit>

    fun acceptShare(chatMsg : ChatMessage) : Single<ChatMessage>

}
