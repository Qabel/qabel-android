package de.qabel.qabelbox.sync

import android.accounts.Account
import android.content.*
import android.os.Bundle
import de.qabel.core.config.Identity
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.core.service.ChatService
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.chat.dto.ChatMessageInfo
import de.qabel.qabelbox.chat.notifications.ChatNotificationManager
import de.qabel.qabelbox.helper.Helper
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import java.util.*
import javax.inject.Inject

open class QabelSyncAdapter : AbstractThreadedSyncAdapter, AnkoLogger {

    companion object {
        private val TAG = "QabelSyncAdapter"
    }

    lateinit internal var mContentResolver: ContentResolver
    @Inject lateinit internal var context: Context
    @Inject lateinit internal var contactRepository: ContactRepository
    @Inject lateinit internal var chatService: ChatService
    @Inject lateinit internal var notificationManager: ChatNotificationManager

    private var currentMessages: List<ChatMessageInfo> = ArrayList()

    constructor(context: Context, autoInitialize: Boolean) : super(context, autoInitialize) {
        init(context)
    }

    constructor(
            context: Context,
            autoInitialize: Boolean,
            allowParallelSyncs: Boolean) : super(context, autoInitialize, allowParallelSyncs) {
        init(context)
    }

    private fun init(context: Context) {
        this.context = context
        mContentResolver = context.contentResolver
        QabelBoxApplication.getApplicationComponent(context).inject(this)
        registerNotificationReceiver()
    }

    private fun registerNotificationReceiver() {
        val filter = IntentFilter(Helper.INTENT_SHOW_NOTIFICATION)
        filter.priority = 0
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                notificationManager.updateNotifications(currentMessages)
            }
        }
        context.registerReceiver(receiver, filter)
    }


    override fun onPerformSync(
            account: Account,
            extras: Bundle,
            authority: String,
            provider: ContentProviderClient,
            syncResult: SyncResult) {
        info("Starting drop message sync")

        val newMessageList = try {
            chatService.refreshMessages().
                    flatMap { toChatMessageInfo(it.key, it.value) }
        } catch (e: Throwable) {
            warn("Error in syncing", e)
            emptyList<ChatMessageInfo>()
        }

        notifyForNewMessages(newMessageList)
    }

    open fun notifyForNewMessages(retrievedMessages: List<ChatMessageInfo>) {
        if (retrievedMessages.size == 0) {
            return
        }
        currentMessages = retrievedMessages

        val notificationIntent = Intent(Helper.INTENT_SHOW_NOTIFICATION)
        val pairs = retrievedMessages.flatMap { listOf(it.identity.keyIdentifier, it.contact.keyIdentifier) }.filterNotNull()
        notificationIntent.putStringArrayListExtra(Helper.AFFECTED_IDENTITIES_AND_CONTACTS, ArrayList(pairs))

        context.sendOrderedBroadcast(notificationIntent, null)
        val refreshList = Intent(Helper.INTENT_REFRESH_CONTACTLIST)
        context.sendBroadcast(refreshList)
        val refreshChat = Intent(Helper.INTENT_REFRESH_CHAT)
        context.sendBroadcast(refreshChat)
    }

    private fun toChatMessageInfo(identity: Identity, newMessages: List<ChatDropMessage>): List<ChatMessageInfo> {
        return newMessages.map {
            val contact = contactRepository.find(it.contactId)
            ChatMessageInfo.fromChatDropMessage(identity, contact, it)
        }
    }


}
