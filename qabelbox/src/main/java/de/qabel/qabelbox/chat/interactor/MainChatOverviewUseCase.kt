package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Identity
import de.qabel.core.repository.ChatDropMessageRepository
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer
import rx.lang.kotlin.observable
import javax.inject.Inject


class MainChatOverviewUseCase @Inject constructor(private val chatRepo: ChatDropMessageRepository,
                                                  private val chatMessageTransformer: ChatMessageTransformer) : ChatOverviewUseCase {

    override fun findLatest(identity: Identity) = observable<ChatMessage> { subscriber ->
        chatRepo.findLatest(identity.id).map {
            try {
                subscriber.onNext(chatMessageTransformer.transform(it))
            }catch (ex : Throwable){}
        }
        subscriber.onCompleted()
    }

}
