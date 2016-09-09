package de.qabel.qabelbox.index

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.qabel.qabelbox.chat.services.AndroidChatService

class IndexIdentityListener() : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.applicationContext.startService(createServiceIntent(context, intent))
    }

    private fun createServiceIntent(context: Context, intent: Intent): Intent =
            Intent(intent.action, null, context.applicationContext, AndroidChatService::class.java).apply {
                putExtras(intent.extras)
            }
}
