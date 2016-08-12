package de.qabel.qabelbox.chat.notifications.presenter

import de.qabel.qabelbox.chat.notifications.ChatNotification

interface ChatNotificationPresenter {

    fun showNotification(notification: ChatNotification)

    fun hideNotification(identityKey: String)
    fun hideNotification(identityKey: String, contactKey : String)

}
