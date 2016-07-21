package de.qabel.qabelbox.chat.view.presenters

interface ChatPresenter {

    fun refreshMessages()

    val title: String

    fun sendMessage()

}
