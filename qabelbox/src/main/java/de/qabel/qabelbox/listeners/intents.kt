package de.qabel.qabelbox.listeners

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.qabel.core.extensions.letApply
import de.qabel.qabelbox.base.BaseActivity
import de.qabel.qabelbox.base.BaseFragment

data class IntentListener(val action: String, val receiver: BroadcastReceiver, var priority: Int = 0)

private fun createListener(handleIntent: (intent: Intent) -> Unit) = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { handleIntent(it) }
    }
}

internal fun IntentListener.toIntentFilter() = IntentFilter(action).letApply {
    it.priority = priority
}

internal fun BaseFragment.intentListener(action: String, handleIntent: (Intent) -> Unit) =
        IntentListener(action, createListener(handleIntent))

internal fun BaseActivity.intentListener(action: String, handleIntent: (Intent) -> Unit) =
        IntentListener(action, createListener(handleIntent))
