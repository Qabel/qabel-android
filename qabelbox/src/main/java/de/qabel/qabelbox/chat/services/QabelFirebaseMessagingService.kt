package de.qabel.qabelbox.chat.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.chat.interactor.Base64DropReceiver
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import javax.inject.Inject

class QabelFirebaseMessagingService: FirebaseMessagingService(), AnkoLogger {


    @Inject lateinit var receiver: Base64DropReceiver

    override fun onCreate() {
        super.onCreate()
        QabelBoxApplication.getApplicationComponent(applicationContext).inject(this)
        info("QabelFireBaseMessagingService started")
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        info("Message received")
        message?.let {
            val dropId = it.data["drop-id"] ?: return
            val msg =  it.data["message"] ?: return
            receiver.receive(dropId, msg)
            return
        }
        warn("Could not receive message: $message")

    }


}

