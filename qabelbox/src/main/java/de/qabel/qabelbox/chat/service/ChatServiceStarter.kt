package de.qabel.qabelbox.chat.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.qabel.qabelbox.QblBroadcastConstants
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


class ChatServiceStarter : BroadcastReceiver(), AnkoLogger {

    override fun onReceive(context: Context, intent: Intent?) {
        info("Receive notify broadcast. Start service")
        context.applicationContext.startService(Intent(QblBroadcastConstants.Chat.Service.NOTIFY,
                null, context.applicationContext, AndroidChatService::class.java))
    }

}
