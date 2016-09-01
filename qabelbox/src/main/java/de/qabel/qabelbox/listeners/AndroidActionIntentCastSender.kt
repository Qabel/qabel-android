package de.qabel.qabelbox.listeners

import android.content.Context
import android.content.Intent

class AndroidActionIntentCastSender(private val ctx: Context) : ActionIntentSender {

    override fun sendActionIntentBroadCast(action: String, vararg params: Pair<String, Any>) {
        ctx.sendBroadcast(Intent(action).apply {
            params.forEach {
                when (it.second) {
                    is String -> putExtra(it.first, it.second as String)
                    is Int -> putExtra(it.first, it.second as Int)
                    is Long -> putExtra(it.first, it.second as Long)
                    is Boolean -> putExtra(it.first, it.second as Boolean)
                    is IntArray -> putExtra(it.first, it.second as IntArray)
                    else -> throw RuntimeException("Invalid broadcast param type ${it.second.javaClass.simpleName}")
                }
            }
        })
    }

}
