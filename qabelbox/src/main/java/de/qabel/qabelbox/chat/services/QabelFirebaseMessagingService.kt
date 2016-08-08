package de.qabel.qabelbox.chat.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.helper.AccountHelper
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn

class QabelFirebaseMessagingService: FirebaseMessagingService(), AnkoLogger {


    override fun onCreate() {
        super.onCreate()
        QabelBoxApplication.getApplicationComponent(applicationContext).inject(this)
        info("QabelFireBaseMessagingService started")
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        info("Message received")
        if (message == null) {
            warn("message is null")
            return
        }
        val dropId = message.data["drop-id"]
        if (dropId != null) {
            info("drop id is $dropId, polling")
            AccountHelper.startOnDemandSyncAdapter()
        } else {
            warn("no drop id found in message")
        }
    }


}

