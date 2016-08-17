package de.qabel.qabelbox.chat.view.views

import de.qabel.core.config.Identity
import de.qabel.qabelbox.chat.dto.ChatMessage

interface ChatOverview {

    var identity: Identity

    fun loadData(data: List<ChatMessage>)

}
