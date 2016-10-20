package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Identity

interface MessageStateBroadcaster {
    fun messagesRead(identity: Identity)
}

