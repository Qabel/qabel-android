package de.qabel.qabelbox.chat.interactor


interface MarkAsRead {
    fun all(identityKey: String)
    fun forContact(identityKey: String, contactKey: String)
}

