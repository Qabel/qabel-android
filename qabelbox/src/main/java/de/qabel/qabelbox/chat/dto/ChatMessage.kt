package de.qabel.qabelbox.chat.dto

import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import java.util.*

data class ChatMessage(val identity: Identity, val contact: Contact, val direction: ChatDropMessage.Direction,
                       val time: Date, val messagePayload: MessagePayloadDto) {
}


