package de.qabel.qabelbox.chat.view.presenters

interface ChatPresenter {

    fun refreshMessages()

    val title: String

    val subtitle: String

    val showContactMenu : Boolean

    fun sendMessage()

    fun handleHeaderClick()

    fun handleContactAddClick()

    fun handleContactIgnoreClick()

}
