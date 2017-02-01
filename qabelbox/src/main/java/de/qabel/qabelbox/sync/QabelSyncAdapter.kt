package de.qabel.qabelbox.sync

import android.accounts.Account
import android.content.*
import android.os.Bundle
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.chat.service.ChatService
import de.qabel.chat.service.SharingService
import de.qabel.core.config.Contact
import de.qabel.core.repository.ContactRepository
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.chat.services.AndroidChatService
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.reporter.CrashReporter
import de.qabel.qabelbox.reporter.CrashSubmitter
import de.qabel.qabelbox.storage.server.BlockServer
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import javax.inject.Inject

open class QabelSyncAdapter : AbstractThreadedSyncAdapter, AnkoLogger {

    lateinit internal var mContentResolver: ContentResolver
    @Inject lateinit internal var context: Context
    @Inject lateinit internal var contactRepository: ContactRepository
    @Inject lateinit internal var chatService: ChatService
    @Inject lateinit internal var sharingService: SharingService
    @Inject lateinit internal var blockServer: BlockServer
    @Inject lateinit internal var preferences: AppPreference
    @Inject lateinit internal var crashReporter: CrashReporter
    @Inject lateinit internal var crashSubmitter: CrashSubmitter

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
        crashReporter.installCrashReporter()
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
            if (result.isNotEmpty()) {
                result.forEach {
                    val (identityKey, messages) = it
                    messages.forEach {
                        if (it.messageType == ChatDropMessage.MessageType.SHARE_NOTIFICATION) {
                            val contact = contactRepository.find(it.contactId)
                            if (contact.status != Contact.ContactStatus.UNKNOWN) {
                                sharingService.acceptShare(it, BoxHttpStorageBackend(blockServer, ""))
                            }
                        }
                    }
                }
                context.applicationContext.startService(Intent(QblBroadcastConstants.Chat.Service.MESSAGES_UPDATED,
                        null, context.applicationContext, AndroidChatService::class.java))
                info("ChatService Intent sent")
            }
        } catch(ex: Throwable) {
            warn("Error on syncing dropMessages", ex)
            crashSubmitter.submit(ex)
        }
    }
}
