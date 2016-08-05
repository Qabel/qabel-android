package de.qabel.qabelbox.chat.interactor

interface DropReceiver {
    fun receive(dropId: String, encodedMessage: String)
}
