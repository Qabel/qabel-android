package de.qabel.qabelbox.chat.dto

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.repository.entities.ChatDropMessage
import java.util.*

data class ChatMessage(val identity: Identity, val contact: Contact, val direction: ChatDropMessage.Direction,
                       val time: Date, val messagePayload: MessagePayloadDto) {
}


