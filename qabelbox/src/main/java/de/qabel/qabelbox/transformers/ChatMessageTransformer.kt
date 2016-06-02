package de.qabel.qabelbox.transformers

import de.qabel.desktop.repository.ContactRepository
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion
import de.qabel.qabelbox.chat.ChatMessageItem
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.dto.MessagePayload
import de.qabel.qabelbox.dto.SymmetricKey
import java.net.URL
import java.util.*
import javax.inject.Inject

class ChatMessageTransformer @Inject constructor(
        private val identityRepository: IdentityRepository,
        private val contactRepository: ContactRepository) {

    fun transform(chatMessageItem: ChatMessageItem): ChatMessage {
        val time = Date(chatMessageItem.time)
        val (direction, identity) = try {
            Pair(ChatMessage.Direction.INCOMING, identityRepository.find(chatMessageItem.receiver))
        } catch (e: EntityNotFoundExcepion) {
            Pair(ChatMessage.Direction.OUTGOING, identityRepository.find(chatMessageItem.sender))
        }
        val contact = contactRepository.findByKeyId(identity,
                if (direction == ChatMessage.Direction.INCOMING) {
                    chatMessageItem.sender
                } else {
                    chatMessageItem.receiver
                })
        return ChatMessage(identity, contact, direction, time, extractPayload(chatMessageItem.data))
    }

    private fun extractPayload(messagePayload: ChatMessageItem.MessagePayload): MessagePayload {
        return when (messagePayload) {
            is ChatMessageItem.TextMessagePayload -> MessagePayload.TextMessage(messagePayload.message)
            is ChatMessageItem.ShareMessagePayload -> MessagePayload.ShareMessage(
                    messagePayload.message, URL(messagePayload.url),
                    SymmetricKey.Factory.fromHex(messagePayload.key))
            else -> MessagePayload.NoMessage
        }
    }
}
