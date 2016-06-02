package de.qabel.qabelbox.dto

import java.net.URL

sealed class MessagePayload {
    object NoMessage: MessagePayload()
    class TextMessage(val message: String) : MessagePayload()
    class ShareMessage(val message: String, val url: URL, val key: SymmetricKey) : MessagePayload()
}

