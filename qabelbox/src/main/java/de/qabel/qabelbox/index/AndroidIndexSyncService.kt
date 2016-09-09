package de.qabel.qabelbox.index

import android.app.IntentService
import android.content.Context
import android.content.Intent
import de.qabel.core.config.Identity
import de.qabel.core.config.VerificationStatus
import de.qabel.core.index.ExternalContactsAccessor
import de.qabel.core.index.IndexInteractor
import de.qabel.core.index.IndexSyncAction
import de.qabel.core.logging.QabelLog
import de.qabel.core.logging.debug
import de.qabel.core.logging.error
import de.qabel.core.logging.info
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.QblBroadcastConstants.Identities.*
import de.qabel.qabelbox.QblBroadcastConstants.Index.REQUEST_VERIFICATION
import de.qabel.qabelbox.QblBroadcastConstants.Index.SYNC_CONTACTS
import de.qabel.qabelbox.chat.services.AndroidChatService
import de.qabel.qabelbox.index.dagger.IndexModule
import javax.inject.Inject

class AndroidIndexSyncService() : IntentService(AndroidIndexSyncService::class.java.simpleName), QabelLog {

    @Inject lateinit var indexInteractor: IndexInteractor
    @Inject lateinit var contactsAccessor: ExternalContactsAccessor

    companion object {
        private fun start(context: Context, action: String) {
            context.startService(Intent(action, null, context.applicationContext, AndroidIndexSyncService::class.java))
        }

        fun startSyncVerifications(context: Context) =
                start(context, QblBroadcastConstants.Index.SYNC_VERIFICATIONS)

        fun startSyncContacts(context: Context) =
                start(context, QblBroadcastConstants.Index.SYNC_CONTACTS)
    }

    override fun onCreate() {
        super.onCreate()
        QabelBoxApplication.getApplicationComponent(applicationContext).plus(IndexModule()).inject(this)
        info("Service initialized!")
    }

    private fun Intent.affectedIdentity(): Identity = getSerializableExtra(QblBroadcastConstants.Identities.KEY_IDENTITY) as Identity
    private fun Intent.outdatedIdentity(): Identity = getSerializableExtra(QblBroadcastConstants.Identities.OLD_IDENTITY) as Identity

    override fun onHandleIntent(intent: Intent) {
        debug("IndexSync received intent with action ${intent.action}")
        try {
            when (intent.action) {
                IDENTITY_CHANGED -> {
                    val identity = intent.affectedIdentity()
                    val oldIdentity = intent.outdatedIdentity()
                    if (identity.email != oldIdentity.email) {
                        indexInteractor.updateIdentityEmail(identity, oldIdentity.email)
                    }
                    if (identity.phone != oldIdentity.phone) {
                        indexInteractor.updateIdentityPhone(identity, oldIdentity.phone)
                    }
                    sendRequestIntentIfRequiresPhoneVerification(identity)
                }
                IDENTITY_CREATED -> {
                    val identity = intent.affectedIdentity()
                    indexInteractor.updateIdentity(identity)
                    sendRequestIntentIfRequiresPhoneVerification(identity)
                }
                IDENTITY_REMOVED -> indexInteractor.deleteIdentity(intent.affectedIdentity())
                SYNC_CONTACTS -> {
                    val syncResults = indexInteractor.syncContacts(contactsAccessor)
                    if (syncResults.isNotEmpty()) {
                        applicationContext.sendBroadcast(Intent(QblBroadcastConstants.Contacts.CONTACTS_CHANGED))
                        val grouped = syncResults.groupBy { it.action }
                        val createdCount = grouped[IndexSyncAction.CREATE]?.size ?: 0
                        val updatedCount = grouped[IndexSyncAction.UPDATE]?.size ?: 0
                        info("ContactSync completed! Created: $createdCount, Updated: $updatedCount")
                    }
                }
            }
        } catch (ex: Throwable) {
            error("Error syncing with index. Action: ${intent.action}", ex)
        }
    }

    private fun sendRequestIntentIfRequiresPhoneVerification(identity: Identity) {
        if (identity.phoneStatus == VerificationStatus.NOT_VERIFIED) {
            sendBroadcast(Intent(REQUEST_VERIFICATION).apply {
                putExtra(KEY_IDENTITY, identity)
            })
        }
    }

}
