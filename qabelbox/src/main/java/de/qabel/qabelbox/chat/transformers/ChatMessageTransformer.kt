package de.qabel.qabelbox.chat.transformers

import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import java.util.*
import javax.inject.Inject

open class ChatMessageTransformer @Inject constructor(
        private val identityRepository: IdentityRepository,
        private val contactRepository: ContactRepository) {

    fun transform(chatMessageItem: ChatDropMessage): ChatMessage {
        val time = Date(chatMessageItem.createdOn)
        val identity = identityRepository.find(chatMessageItem.identityId)
        val contact = contactRepository.find(chatMessageItem.contactId)
        val payload = chatMessageItem.payload
        return ChatMessage(identity, contact, chatMessageItem.direction, time, when (payload) {
            is ChatDropMessage.MessagePayload.ShareMessage -> payload.toPayloadDto()
            is ChatDropMessage.MessagePayload.TextMessage -> payload.toPayloadDto()
            else -> MessagePayloadDto.NoMessageDto
        })
    }

    fun ChatDropMessage.MessagePayload.ShareMessage.toPayloadDto() = MessagePayloadDto.ShareMessage(msg, shareData)
    fun ChatDropMessage.MessagePayload.TextMessage.toPayloadDto() = MessagePayloadDto.TextMessage(msg)

}
