package de.qabel.qabelbox.chat.service

import android.app.IntentService
import android.content.Intent
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.QblBroadcastConstants.Chat.Service
import de.qabel.qabelbox.chat.interactor.ChatServiceUseCase
import de.qabel.qabelbox.chat.notifications.ChatNotificationManager
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer
import de.qabel.qabelbox.helper.Helper
import de.qabel.qabelbox.navigation.MainNavigator
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*
import javax.inject.Inject

open class AndroidChatService() : IntentService(AndroidChatService::class.java.simpleName), AnkoLogger {

    companion object {
        const val PARAM_CONTACT_KEY = "contact_key"
        const val PARAM_IDENTITY_KEY = "identity_key"
    }

    private fun Intent.contactKey(): String = getStringExtra(PARAM_CONTACT_KEY)
    private fun Intent.identityKey(): String = getStringExtra(PARAM_IDENTITY_KEY)

    @Inject lateinit var chatService: ChatServiceUseCase
    @Inject lateinit var chatMessageTransformer: ChatMessageTransformer
    @Inject lateinit var chatNotificationManager: ChatNotificationManager

    override fun onCreate() {
        super.onCreate()
        QabelBoxApplication.getApplicationComponent(applicationContext).inject(this)
        info("Service initialized!")
    }

    override public fun onHandleIntent(intent: Intent) {
        info("Received chat service intent " + intent.action)
        when (intent.action) {
            Service.MESSAGES_UPDATED -> {
                val affectedKeys = chatService.getNewMessageAffectedKeyIds()

                Intent(QblBroadcastConstants.Chat.INTENT_SHOW_NOTIFICATION).let {
                    it.putStringArrayListExtra(Helper.AFFECTED_IDENTITIES_AND_CONTACTS, ArrayList(affectedKeys))
                    applicationContext.sendOrderedBroadcast(it, null)
                }
                info("NewMessages broadcast sent (" + affectedKeys.size + ")")
            }
            Service.NOTIFY -> updateNotification()
            Service.MARK_READ -> {
                val identityKey = intent.identityKey()
                if (intent.hasExtra(PARAM_CONTACT_KEY)) {
                    val contactKey = intent.contactKey()
                    chatService.markContactMessagesRead(identityKey, contactKey)
                    chatNotificationManager.hideNotification(identityKey, contactKey)
                } else {
                    chatService.markIdentityMessagesRead(identityKey)
                    chatNotificationManager.hideNotification(identityKey, null)
                }
            }
            Service.ADD_CONTACT -> {
                val identityKey = intent.identityKey()
                val contactKey = intent.contactKey()
                chatService.addContact(identityKey, contactKey)
                chatNotificationManager.hideNotification(identityKey, contactKey)
                sendContactsUpdated()
                applicationContext.startActivity(MainNavigator.createChatIntent(applicationContext,
                        intent.identityKey(), intent.contactKey()).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            Service.IGNORE_CONTACT -> {
                val identityKey = intent.identityKey()
                val contactKey = intent.contactKey()
                chatService.ignoreContact(identityKey, contactKey)
                chatService.markContactMessagesRead(identityKey, contactKey)
                chatNotificationManager.hideNotification(identityKey, contactKey)
                sendContactsUpdated()
            }
        }
    }

    private fun sendChatRefresh() =
            applicationContext.sendBroadcast(Intent(QblBroadcastConstants.Chat.REFRESH))

    private fun sendContactsUpdated() =
            applicationContext.sendBroadcast(Intent(QblBroadcastConstants.Contacts.CONTACTS_CHANGED))


    private fun updateNotification() {
        info("NewMessages broadcast received")
        val newMessages = chatService.getNewMessageMap()
        chatNotificationManager.updateNotifications(newMessages)
        info("NewMessages notified " + newMessages.size)
    }

}
