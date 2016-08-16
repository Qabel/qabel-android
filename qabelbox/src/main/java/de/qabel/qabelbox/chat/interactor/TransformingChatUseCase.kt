package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.repository.ChatDropMessageRepository
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.core.service.ChatService
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer
import rx.Observable
import rx.lang.kotlin.observable
import javax.inject.Inject
import kotlin.concurrent.thread

class TransformingChatUseCase @Inject constructor(val identity: Identity, override val contact: Contact,
                                                  private val chatMessageTransformer: ChatMessageTransformer,
                                                  private val chatService: ChatService,
                                                  private val chatDropMessageRepository: ChatDropMessageRepository,
                                                  private val chatServiceUseCase: ChatServiceUseCase) : ChatUseCase {

    override fun send(text: String): Observable<ChatMessage> = observable { subscriber ->
        val item = ChatDropMessage(contact.id, identity.id,
                ChatDropMessage.Direction.OUTGOING, ChatDropMessage.Status.PENDING,
                ChatDropMessage.MessageType.BOX_MESSAGE, ChatDropMessage.MessagePayload.TextMessage(text),
                System.currentTimeMillis())

        subscriber.onNext(chatMessageTransformer.transform(item))
        chatService.sendMessage(item)
        subscriber.onCompleted()
    }

    override fun retrieve() = observable<ChatMessage> { subscriber ->
        chatDropMessageRepository.markAsRead(contact, identity)
        chatDropMessageRepository.findByContact(contact.id, identity.id).forEach { msg ->
            subscriber.onNext(chatMessageTransformer.transform(msg))
        }
        subscriber.onCompleted()
    }

    override fun addContact() = observable<Unit> { subscriber ->
        chatServiceUseCase.addContact(identity.keyIdentifier, contact.keyIdentifier)
        subscriber.onNext(Unit)
        subscriber.onCompleted()
    }

    override fun ignoreContact() = observable<Unit> { subscriber ->
        chatServiceUseCase.ignoreContact(identity.keyIdentifier, contact.keyIdentifier)
        subscriber.onNext(Unit)
        subscriber.onCompleted()
    }

}

