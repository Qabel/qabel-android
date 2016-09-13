package de.qabel.qabelbox.index

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.qabel.qabelbox.QblBroadcastConstants.Identities.*
import de.qabel.qabelbox.QblBroadcastConstants.Index.*

class IndexIdentityListener() : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.applicationContext.startService(createServiceIntent(context, intent))
    }

    private fun createServiceIntent(context: Context, intent: Intent): Intent =
            Intent(intent.action, null, context.applicationContext, AndroidIndexSyncService::class.java).apply {
                putExtras(intent.extras)
            }

    fun createIntentFilter(): IntentFilter =
            IntentFilter().apply {
                addAction(IDENTITY_CHANGED)
                addAction(IDENTITY_CREATED)
                addAction(IDENTITY_REMOVED)
                addAction(IDENTITY_UPLOAD_ENABLED)
                addAction(IDENTITY_UPLOAD_DISABLED)
            }
}
