package de.qabel.qabelbox.ui.presenters

interface ChatPresenter {

    fun refreshMessages()

    val title: String

    fun sendMessage()

}
