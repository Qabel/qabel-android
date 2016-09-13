package de.qabel.qabelbox.chat.dto

import de.qabel.chat.repository.entities.BoxFileChatShare
import de.qabel.core.config.SymmetricKey
import java.net.URI

sealed class MessagePayloadDto {
    object NoMessageDto : MessagePayloadDto()
    class TextMessage(val message: String) : MessagePayloadDto()
    class ShareMessage(val message: String, val share : BoxFileChatShare) : MessagePayloadDto()

    fun toMessage(): String =
            when (this) {
                is MessagePayloadDto.TextMessage -> this.message
                is MessagePayloadDto.ShareMessage -> this.message
                else -> ""
            }
}

