package de.qabel.qabelbox.chat.notifications

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.contacts.extensions.displayName
import java.util.*


data class MessageChatNotification(override val identity: Identity,
                                   override val header: String,
                                   override val message: String,
                                   override val date: Date) : ChatNotification {
}

data class ContactChatNotification(override val identity: Identity,
                                   val contact: Contact,
                                   override val message: String,
                                   override val date: Date) : ChatNotification {
    override val header: String = contact.displayName()
    var extraNotification = false
}
