package de.qabel.qabelbox.chat.dto

import java.net.URL

sealed class MessagePayloadDto {
    object NoMessageDto : MessagePayloadDto()
    class TextMessage(val message: String) : MessagePayloadDto()
    class ShareMessage(val message: String, val url: URL, val key: SymmetricKey, val status : ShareStatus = ShareStatus.ACCEPTED) : MessagePayloadDto(){
        enum class ShareStatus {
            NEW, ACCEPTED, NOT_REACHABLE, DELETED
        }
    }
}

