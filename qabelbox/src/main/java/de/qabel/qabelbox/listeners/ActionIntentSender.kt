package de.qabel.qabelbox.listeners

interface ActionIntentSender {

    fun sendActionIntentBroadCast(action: String, vararg params: Pair<String, Any>)

}
