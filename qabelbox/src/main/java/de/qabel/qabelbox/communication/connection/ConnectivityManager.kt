package de.qabel.qabelbox.communication.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

open class ConnectivityManager(private val context: Context) {

    private val connectivityManager: android.net.ConnectivityManager?

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isConnected) {
                listener?.handleConnectionEstablished()
            } else {
                listener?.handleConnectionLost()
            }
        }
    }

    var listener: ConnectivityListener? = null

    init {
        this.connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
        context.registerReceiver(broadcastReceiver,
                IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION))
    }

    fun onDestroy() {
        listener?.onDestroy()
        context.unregisterReceiver(broadcastReceiver)
    }

    open val isConnected: Boolean
        get() = connectivityManager?.activeNetworkInfo?.isConnected ?: false

    val isWifi: Boolean
        get() = connectivityManager?.activeNetworkInfo?.type == android.net.ConnectivityManager.TYPE_WIFI


    interface ConnectivityListener {

        fun handleConnectionLost()

        fun handleConnectionEstablished()

        fun onDestroy()
    }
}
