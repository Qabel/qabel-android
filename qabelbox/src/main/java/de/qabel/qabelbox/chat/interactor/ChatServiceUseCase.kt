package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Identity
import de.qabel.qabelbox.chat.dto.ChatMessage

interface ChatServiceUseCase {

    fun addContact(identityKey: String, contactKey: String)
    fun ignoreContact(identityKey: String, contactKey: String)

    fun getNewMessageAffectedKeyIds() : Collection<String>
    fun getNewMessageMap(): Map<Identity, List<ChatMessage>>

}

