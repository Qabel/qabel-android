package de.qabel.qabelbox.interactor

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.chat.ChatServer
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.transformers.ChatMessageTransformer
import rx.lang.kotlin.observable
import javax.inject.Inject

class ChatUseCase @Inject constructor(val identity: Identity, val contact: Contact,
                                      private val chatMessageTransformer: ChatMessageTransformer,
                                      private val chatServer: ChatServer) {

    fun retrieve() = observable<List<ChatMessage>> { subscriber ->
        val messages = chatServer.getAllMessages(identity, contact)?.map { msg ->
            chatMessageTransformer.transform(msg)
        } ?: listOf()
        subscriber.onNext(messages)
        subscriber.onCompleted()
    }
}

