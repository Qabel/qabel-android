package de.qabel.qabelbox.chat.interactor

import de.qabel.chat.repository.ChatDropMessageRepository
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.chat.service.ChatService
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.repository.framework.PagingResult
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer
import rx.Observable
import rx.lang.kotlin.observable
import rx.schedulers.Schedulers
import javax.inject.Inject

class TransformingChatUseCase @Inject constructor(val identity: Identity, override val contact: Contact,
                                                  private val chatMessageTransformer: ChatMessageTransformer,
                                                  private val chatService: ChatService,
                                                  private val chatDropMessageRepository: ChatDropMessageRepository,
                                                  private val chatServiceUseCase: ChatServiceUseCase) : ChatUseCase {

    override fun send(text: String): Observable<ChatMessage> = observable<ChatMessage> { subscriber ->
        val item = ChatDropMessage(contact.id, identity.id,
                ChatDropMessage.Direction.OUTGOING, ChatDropMessage.Status.PENDING,
                ChatDropMessage.MessageType.BOX_MESSAGE, ChatDropMessage.MessagePayload.TextMessage(text),
                System.currentTimeMillis())

        subscriber.onNext(chatMessageTransformer.transform(item))
        chatService.sendMessage(item)
        subscriber.onCompleted()
    }.subscribeOn(Schedulers.io())

    override fun load(offset: Int, pageSize: Int) = observable<PagingResult<ChatMessage>> { subscriber ->
        if (offset == 0) {
            chatDropMessageRepository.markAsRead(contact, identity)
        }
        chatDropMessageRepository.findByContact(contact.id, identity.id, offset, pageSize).let { pagingResult ->
            subscriber.onNext(pagingResult.transform { chatMessageTransformer.transform(it) })
        }
        subscriber.onCompleted()
    }

    fun <X, T> PagingResult<T>.transform(transformer: (T) -> X): PagingResult<X> =
            PagingResult(availableRange, result.map { transformer(it) })

    override fun addContact() = observable<Unit> { subscriber ->
        chatServiceUseCase.addContact(identity.keyIdentifier, contact.keyIdentifier)
        contact.status = Contact.ContactStatus.NORMAL
        subscriber.onNext(Unit)
        subscriber.onCompleted()
    }

    override fun ignoreContact() = observable<Unit> { subscriber ->
        chatServiceUseCase.ignoreContact(identity.keyIdentifier, contact.keyIdentifier)
        contact.status = Contact.ContactStatus.NORMAL
        contact.isIgnored = true
        subscriber.onNext(Unit)
        subscriber.onCompleted()
    }

}

