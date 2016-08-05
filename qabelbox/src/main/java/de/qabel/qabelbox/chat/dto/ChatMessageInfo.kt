package de.qabel.qabelbox.chat.dto

import java.util.Date

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.repository.entities.ChatDropMessage

data class ChatMessageInfo(var contact: Contact, var identity: Identity,
                      var message: String, var sent: Date, var type: ChatMessageInfo.MessageType):
        Comparable<ChatMessageInfo> {

    override fun compareTo(other: ChatMessageInfo): Int {
        return sent.compareTo(other.sent)
    }


    enum class MessageType {
        MESSAGE, SHARE
    }

    companion object {
        fun fromChatDropMessage(identity: Identity, contact: Contact, message: ChatDropMessage): ChatMessageInfo {
            val typesValues = when (message.payload) {
                is ChatDropMessage.MessagePayload.ShareMessage -> Pair((message.payload as ChatDropMessage.MessagePayload.ShareMessage).msg, ChatMessageInfo.MessageType.SHARE)
                else -> Pair((message.payload as ChatDropMessage.MessagePayload.TextMessage).msg, ChatMessageInfo.MessageType.MESSAGE)
            }
            return ChatMessageInfo(contact, identity, typesValues.first, Date(message.createdOn), typesValues.second)
        }

    }
}
