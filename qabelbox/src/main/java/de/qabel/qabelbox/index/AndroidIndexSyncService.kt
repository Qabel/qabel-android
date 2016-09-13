package de.qabel.qabelbox.index

import android.app.IntentService
import android.content.Context
import android.content.Intent
import de.qabel.core.config.Identity
import de.qabel.core.config.VerificationStatus
import de.qabel.core.index.IndexService
import de.qabel.core.index.IndexSyncAction
import de.qabel.core.index.server.ExternalContactsAccessor
import de.qabel.core.logging.QabelLog
import de.qabel.core.logging.debug
import de.qabel.core.logging.error
import de.qabel.core.logging.info
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.QblBroadcastConstants.Identities.*
import de.qabel.qabelbox.QblBroadcastConstants.Index.*
import de.qabel.qabelbox.index.dagger.IndexModule
import de.qabel.qabelbox.index.preferences.IndexPreferences
import javax.inject.Inject

class AndroidIndexSyncService() : IntentService(AndroidIndexSyncService::class.java.simpleName), QabelLog {

    @Inject lateinit var indexService: IndexService
    @Inject lateinit var contactsAccessor: ExternalContactsAccessor
    @Inject lateinit var indexPreferences: IndexPreferences

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

    private fun Intent.affectedIdentity(): Identity = getSerializableExtra(KEY_IDENTITY) as Identity
    private fun Intent.outdatedIdentity(): Identity = getSerializableExtra(OLD_IDENTITY) as Identity

    override fun onHandleIntent(intent: Intent) {
        debug("IndexSync received intent with action ${intent.action}")
        try {
            when (intent.action) {
                IDENTITY_CHANGED -> {
                    if (indexPreferences.indexUploadEnabled) {
                        val identity = intent.affectedIdentity()
                        val oldIdentity = intent.outdatedIdentity()
                        indexService.updateIdentity(identity, oldIdentity)
                        sendRequestIntentIfRequiresPhoneVerification(identity)
                    }
                }
                IDENTITY_CREATED -> {
                    if (indexPreferences.indexUploadEnabled) {
                        val identity = intent.affectedIdentity()
                        indexService.updateIdentity(identity)
                        sendRequestIntentIfRequiresPhoneVerification(identity)
                    }
                }
                IDENTITY_REMOVED -> indexService.deleteIdentity(intent.affectedIdentity())
                SYNC_CONTACTS -> {
                    val syncResults = indexService.syncContacts(contactsAccessor)
                    if (syncResults.isNotEmpty()) {
                        applicationContext.sendBroadcast(Intent(QblBroadcastConstants.Contacts.CONTACTS_CHANGED))
                        val grouped = syncResults.groupBy { it.action }
                        val createdCount = grouped[IndexSyncAction.CREATE]?.size ?: 0
                        val updatedCount = grouped[IndexSyncAction.UPDATE]?.size ?: 0
                        info("ContactSync completed! Created: $createdCount, Updated: $updatedCount")
                    }
                }
                IDENTITY_UPLOAD_ENABLED -> {

                }
                IDENTITY_UPLOAD_DISABLED -> {

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
