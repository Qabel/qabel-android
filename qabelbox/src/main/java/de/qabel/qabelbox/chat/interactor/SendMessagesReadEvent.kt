package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Identity

interface SendMessagesReadEvent {
    fun messagesRead(identity: Identity)
}

