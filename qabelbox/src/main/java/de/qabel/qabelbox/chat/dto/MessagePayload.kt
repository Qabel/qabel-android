package de.qabel.qabelbox.chat.dto

import java.net.URL

sealed class MessagePayload {
    object NoMessage: MessagePayload()
    class TextMessage(val message: String) : MessagePayload()
    class ShareMessage(val message: String, val url: URL, val key: SymmetricKey, val status : ShareStatus = ShareStatus.ACCEPTED) : MessagePayload(){
        enum class ShareStatus {
            NEW, ACCEPTED, NOT_REACHABLE, DELETED
        }
    }
}

