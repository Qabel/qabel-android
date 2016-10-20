package de.qabel.qabelbox.chat.interactor

import de.qabel.chat.repository.ChatDropMessageRepository
import de.qabel.core.config.Identity
import de.qabel.qabelbox.chat.dto.ChatConversationDto
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer
import rx.lang.kotlin.observable
import javax.inject.Inject


class MainFindLatestConversations @Inject constructor(private val chatRepo: ChatDropMessageRepository,
                                                      private val chatMessageTransformer: ChatMessageTransformer) : FindLatestConversations {

    override fun findLatest(identity: Identity) = observable<ChatConversationDto> { subscriber ->
        val newMessages = chatRepo.findNew(identity.id)
        chatRepo.findLatest(identity.id).map {
            val msg = chatMessageTransformer.transform(it)
            subscriber.onNext(ChatConversationDto(msg, newMessages.count { it.contactId == msg.contact.id }))
        }
        subscriber.onCompleted()
    }

}
