package de.qabel.qabelbox.account

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import de.qabel.core.logging.QabelLog

import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.communication.BoxAccountRegisterServer
import de.qabel.qabelbox.communication.callbacks.JSONModelCallback
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.storage.data.BoxQuotaJSONAdapter
import de.qabel.qabelbox.storage.model.BoxQuota
import de.qabel.qabelbox.storage.server.BlockServer
import okhttp3.Response

class AccountManager(private val context: Context,
                     private val preferences: AppPreference,
                     private val blockServer: BlockServer) : QabelLog {

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshQuota()
        }
    }

    init {
        context.registerReceiver(broadcastReceiver,
                IntentFilter(QblBroadcastConstants.Storage.BOX_CHANGED))
    }

    fun refreshQuota() {
        debug("Refreshing quota")
        blockServer.getQuota(object : JSONModelCallback<BoxQuota>(
                BoxQuotaJSONAdapter()) {

            override fun onSuccess(response: Response, model: BoxQuota) {
                debug("Received quota ${jsonAdapter.toJson(model)}")
                if (model !== preferences.boxQuota) {
                    preferences.boxQuota = model
                    broadcastAccountChanged(AccountStatusCodes.QUOTA_UPDATED)
                }
            }

            override fun onError(e: Exception, response: Response?) {
                error("Cannot receive quota from blockserver", e)
            }
        })
    }

    private fun broadcastAccountChanged(statusCode: Int) {
        val intent = Intent(QblBroadcastConstants.Account.ACCOUNT_CHANGED)
        intent.putExtra(QblBroadcastConstants.STATUS_CODE_PARAM, statusCode)
        context.sendBroadcast(intent)
    }

    val boxQuota: BoxQuota
        get() {
            val quota = preferences.boxQuota
            try {
                refreshQuota()
            } catch (ex: Throwable) {
                error("Error refreshing quota!", ex)
            }
            return quota
        }

    fun logout() {
        preferences.token = null
        broadcastAccountChanged(AccountStatusCodes.LOGOUT)
    }

}
