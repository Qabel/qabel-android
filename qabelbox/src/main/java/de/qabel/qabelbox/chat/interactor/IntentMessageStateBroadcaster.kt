package de.qabel.qabelbox.chat.interactor

import android.content.Context
import android.content.Intent
import de.qabel.core.config.Identity
import de.qabel.qabelbox.QblBroadcastConstants.Chat.Service.MARK_READ
import de.qabel.qabelbox.chat.services.AndroidChatService
import javax.inject.Inject

class IntentMessageStateBroadcaster @Inject constructor(private val context: Context)
: MessageStateBroadcaster {

    override fun messagesRead(identity: Identity) {
        val intent = Intent(context, AndroidChatService::class.java).apply {
                    action = MARK_READ
                    putExtra(AndroidChatService.PARAM_IDENTITY_KEY, identity.keyIdentifier)
                }
        context.startService(intent)
    }

}

