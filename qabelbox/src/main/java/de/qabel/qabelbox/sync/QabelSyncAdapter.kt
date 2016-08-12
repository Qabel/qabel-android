package de.qabel.qabelbox.sync

import android.accounts.Account
import android.content.*
import android.os.Bundle
import de.qabel.core.repository.ContactRepository
import de.qabel.core.service.ChatService
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.chat.notifications.ChatNotificationManager
import de.qabel.qabelbox.chat.services.AndroidChatService
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import javax.inject.Inject

open class QabelSyncAdapter : AbstractThreadedSyncAdapter, AnkoLogger {

    lateinit internal var mContentResolver: ContentResolver
    @Inject lateinit internal var context: Context
    @Inject lateinit internal var contactRepository: ContactRepository
    @Inject lateinit internal var chatService: ChatService
    @Inject lateinit internal var notificationManager: ChatNotificationManager

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
        info("SyncAdapter initialized")
    }

    override fun onPerformSync(
            account: Account,
            extras: Bundle,
            authority: String,
            provider: ContentProviderClient,
            syncResult: SyncResult) {
        info("Starting drop message sync")
        try {
            val result = chatService.refreshMessages()

            info("Received messages for " + result.size + " identities")
            if (result.size > 0) {
                context.applicationContext.startService(Intent(QblBroadcastConstants.Chat.Service.MESSAGES_UPDATED,
                        null, context.applicationContext, AndroidChatService::class.java))
                info("ChatService Intent sent")
            }
        } catch(ex: Throwable) {
            warn("Error on syncing dropMessages", ex)
        }
    }
}
