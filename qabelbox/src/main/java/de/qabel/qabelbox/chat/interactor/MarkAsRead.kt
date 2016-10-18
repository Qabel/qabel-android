package de.qabel.qabelbox.chat.interactor


interface MarkAsRead {
    fun markIdentityMessagesRead(identityKey: String)
    fun markContactMessagesRead(identityKey: String, contactKey: String)
}

