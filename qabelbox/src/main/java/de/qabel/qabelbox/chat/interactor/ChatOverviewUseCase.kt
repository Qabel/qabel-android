package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Identity
import de.qabel.qabelbox.chat.dto.ChatMessage
import rx.Observable

interface ChatOverviewUseCase {

    fun findLatest(identity: Identity): Observable<ChatMessage>

}
