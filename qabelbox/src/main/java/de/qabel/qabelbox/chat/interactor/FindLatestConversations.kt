package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Identity
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.ChatConversationDto
import rx.Observable

interface FindLatestConversations {

    fun findLatest(identity: Identity): Observable<ChatConversationDto>

}
