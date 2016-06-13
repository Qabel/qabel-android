package de.qabel.qabelbox.adapter

import android.accounts.Account
import android.content.*
import android.os.Bundle
import android.util.Log
import de.qabel.core.config.Identity
import de.qabel.desktop.repository.ContactRepository
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion
import de.qabel.desktop.repository.exception.PersistenceException
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.chat.ChatMessageInfo
import de.qabel.qabelbox.chat.ChatMessageItem
import de.qabel.qabelbox.chat.ChatNotificationManager
import de.qabel.qabelbox.chat.ChatServer
import de.qabel.qabelbox.helper.Helper
import de.qabel.qabelbox.services.DropConnector
import java.util.*
import javax.inject.Inject

open class QabelSyncAdapter : AbstractThreadedSyncAdapter {
    lateinit internal var mContentResolver: ContentResolver
    @Inject lateinit internal var context: Context
    @Inject lateinit internal var identityRepository: IdentityRepository
    @Inject lateinit internal var contactRepository: ContactRepository
    @Inject lateinit internal var notificationManager: ChatNotificationManager
    @Inject lateinit internal var chatServer: ChatServer
    @Inject lateinit internal var dropConnector: DropConnector
    private var currentMessages: List<ChatMessageInfo> = ArrayList()

    constructor(context: Context, autoInitialize: Boolean) : super(context, autoInitialize) {
        init(context)
    }

    @Inject
    fun setDropConnector(dropConnector: DropConnector) {
        this.dropConnector = dropConnector
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
        Log.w(TAG, "Starting drop message sync")
        val identities: Set<Identity>
        try {
            identities = identityRepository.findAll().identities
        } catch (e: PersistenceException) {
            Log.e(TAG, "Sync failed", e)
            return
        }

        val retrievedMessages = ArrayList<ChatMessageItem>()
        for (identity in identities) {
            Log.i(TAG, "Loading messages for identity " + identity.alias)
            retrievedMessages.addAll(
                    chatServer.refreshList(dropConnector, identity))
        }
        notifyForNewMessages(retrievedMessages)
    }

    open fun notifyForNewMessages(retrievedMessages: List<ChatMessageItem>) {
        if (retrievedMessages.size == 0) {
            return
        }
        updateNotificationManager(retrievedMessages)
        val notificationIntent = Intent(Helper.INTENT_SHOW_NOTIFICATION)
        context.sendOrderedBroadcast(notificationIntent, null)
        val refreshList = Intent(Helper.INTENT_REFRESH_CONTACTLIST)
        context.sendBroadcast(refreshList)
        val refreshChat = Intent(Helper.INTENT_REFRESH_CHAT)
        context.sendBroadcast(refreshChat)
    }

    private fun updateNotificationManager(retrievedMessages: List<ChatMessageItem>) {
        currentMessages = toChatMessageInfo(retrievedMessages)
    }

    private fun toChatMessageInfo(retrievedMessages: List<ChatMessageItem>): List<ChatMessageInfo> {
        val messages = ArrayList<ChatMessageInfo>()
        for (msg in retrievedMessages) {
            try {
                val identity = identityRepository.find(msg.receiverKey)
                val contact = contactRepository.findByKeyId(identity, msg.senderKey)
                val messageInfo = ChatMessageInfo(
                        contact,
                        identity,
                        msg.data.message,
                        Date(msg.time),
                        ChatMessageInfo.MessageType.MESSAGE)
                messages.add(messageInfo)
            } catch (entityNotFoundExcepion: EntityNotFoundExcepion) {
                Log.w(TAG, "Could not find contact " + msg.senderKey
                        + " for identity " + msg.receiverKey)
            } catch (entityNotFoundExcepion: PersistenceException) {
                Log.w(TAG, "Could not find contact " + msg.senderKey + " for identity " + msg.receiverKey)
            }

        }
        return messages
    }

    companion object {
        private val TAG = "QabelSyncAdapter"
    }

}
