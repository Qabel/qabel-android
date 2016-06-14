package de.qabel.qabelbox.interactor

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.chat.ChatMessageItem
import de.qabel.qabelbox.chat.ChatServer
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.services.DropConnector
import de.qabel.qabelbox.transformers.ChatMessageTransformer
import rx.Observable
import rx.lang.kotlin.observable
import javax.inject.Inject
import kotlin.concurrent.thread

class TransformingChatUseCase @Inject constructor(val identity: Identity, override val contact: Contact,
                                                  private val chatMessageTransformer: ChatMessageTransformer,
                                                  private val chatServer: ChatServer,
                                                  private val connector: DropConnector) : ChatUseCase{
    override fun send(text: String): Observable<ChatMessage> = observable { subscriber ->
        thread {
            val message = ChatServer.createTextDropMessage(identity, text)
            val item = ChatMessageItem(identity, contact.keyIdentifier,
                    message.dropPayload, message.dropPayloadType)

            subscriber.onNext(chatMessageTransformer.transform(item))
            connector.sendDropMessage(message, contact, identity, { results ->
                if (results.all { it.value }) {
                    chatServer.storeIntoDB(identity, item)
                    subscriber.onCompleted()
                } else {
                    subscriber.onError(Exception("Could not send message"))
                }
            })
        }
    }

    override fun retrieve() = observable<ChatMessage> { subscriber ->
        thread {
            val messages = chatServer.getAllMessages(identity, contact)?.map { msg ->
                chatMessageTransformer.transform(msg)
            } ?: listOf()
            chatServer.setAllMessagesRead(identity, contact)
            messages.map {  subscriber.onNext(it) }
            subscriber.onCompleted()
        }
    }
}

