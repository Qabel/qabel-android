package de.qabel.qabelbox.chat.notifications

import de.qabel.core.config.Identity
import java.util.*


interface ChatNotification {

    val identity: Identity
    val header: String
    val message: String
    val date: Date

}
