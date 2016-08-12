package de.qabel.qabelbox.chat.notifications

import de.qabel.core.config.Identity
import de.qabel.qabelbox.chat.dto.ChatMessage

interface ChatNotificationManager {

    fun updateNotifications(receivedMessagesMap: Map<Identity, List<ChatMessage>>)

    fun hideNotification(identityKey: String, contactKey: String?)

}
