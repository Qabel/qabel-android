package de.qabel.qabelbox.chat.dto

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import java.util.*

data class ChatMessage(val identity: Identity, val contact: Contact, val direction: Direction,
                       val time: Date, val messagePayload: MessagePayload) {
    enum class Direction {
        INCOMING, OUTGOING
    }
}


