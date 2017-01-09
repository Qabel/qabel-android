package de.qabel.qabelbox.chat.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.qabel.qabelbox.QblBroadcastConstants
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn

/**
 * Starts the Service on receiving QblBroadcastConstants.Chat.NOTIFY_NEW_MESSAGES
 * to show Notifications.
 */
class AndroidChatServiceResponder : BroadcastReceiver(), AnkoLogger {

    override fun onReceive(context: Context, intent: Intent?) {
        info("Receive notify broadcast. Start service")
        if (intent == null) {
            warn("AndroidChatServiceResponder called with a null intent")
            return
        }
        var action = QblBroadcastConstants.Chat.Service.MESSAGES_READ
        if (intent.action == QblBroadcastConstants.Chat.NOTIFY_NEW_MESSAGES) {
            action = QblBroadcastConstants.Chat.Service.NOTIFY
            abortBroadcast()
        }
        context.applicationContext.startService(Intent(action,
                null, context.applicationContext, AndroidChatService::class.java).apply {
            intent.extras.let {
                putExtras(it)
            }
        })
    }

}
