package de.qabel.qabelbox.index

import android.app.IntentService
import android.content.Context
import android.content.Intent
import de.qabel.core.config.Identity
import de.qabel.core.config.VerificationStatus
import de.qabel.core.index.IndexService
import de.qabel.core.index.IndexSyncAction
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.QblBroadcastConstants.Contacts
import de.qabel.qabelbox.QblBroadcastConstants.Identities.*
import de.qabel.qabelbox.QblBroadcastConstants.Index
import de.qabel.qabelbox.QblBroadcastConstants.Index.*
import de.qabel.qabelbox.index.preferences.IndexPreferences
import de.qabel.qabelbox.permissions.DataPermissionsAdapter
import de.qabel.qabelbox.permissions.hasContactsReadPermission
import javax.inject.Inject

class AndroidIndexSyncService() : IntentService(AndroidIndexSyncService::class.java.simpleName),
        QabelLog, DataPermissionsAdapter {

    override val permissionContext: Context by lazy { applicationContext }
    @Inject lateinit var indexService: IndexService
    @Inject lateinit var indexPrefs: IndexPreferences

    companion object {
        private fun start(context: Context, action: String) {
            context.startService(Intent(action, null, context.applicationContext, AndroidIndexSyncService::class.java))
        }

        fun startSyncVerifications(context: Context) =
                start(context, Index.SYNC_VERIFICATIONS)

        fun startSyncContacts(context: Context) =
                start(context, Index.SYNC_CONTACTS)
    }

    override fun onCreate() {
        super.onCreate()
        QabelBoxApplication.getApplicationComponent(applicationContext).indexComponent().inject(this)
        info("Service initialized!")
    }

    private fun Intent.affectedIdentity(): Identity = getSerializableExtra(KEY_IDENTITY) as Identity
    private fun Intent.outdatedIdentity(): Identity = getSerializableExtra(OLD_IDENTITY) as Identity

    override fun onHandleIntent(intent: Intent) {
        debug("IndexSync received intent with action ${intent.action}")
        try {
            when (intent.action) {
                IDENTITY_CHANGED -> handleIdentityChanged(intent)
                IDENTITY_CREATED -> handleIdentityCreated(intent)
                IDENTITY_REMOVED -> handleRemoveIdentity(intent)
                SYNC_CONTACTS -> handleSyncContacts()
                IDENTITY_UPLOAD_ENABLED -> indexService.updateIdentities()
                IDENTITY_UPLOAD_DISABLED -> indexService.removeIdentities()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            error("Error syncing with index. Action: ${intent.action}", ex)
        }
    }

    private fun handleIdentityCreated(intent: Intent) {
        if (indexPrefs.indexUploadEnabled) {
            val identity = intent.affectedIdentity()
            indexService.updateIdentity(identity)
            sendRequestIntentIfRequiresPhoneVerification(identity)
        }
    }

    private fun handleIdentityChanged(intent: Intent) {
        if (indexPrefs.indexUploadEnabled) {
            val identity = intent.affectedIdentity()
            val oldIdentity = intent.outdatedIdentity()
            indexService.updateIdentity(identity, oldIdentity)
            sendRequestIntentIfRequiresPhoneVerification(identity)
        }
    }

    private fun handleRemoveIdentity(intent: Intent) {
        if (indexPrefs.indexUploadEnabled)
            indexService.removeIdentity(intent.affectedIdentity())
    }

    private fun sendRequestIntentIfRequiresPhoneVerification(identity: Identity) {
        if (identity.phoneStatus == VerificationStatus.NOT_VERIFIED) {
            sendBroadcast(Intent(REQUEST_VERIFICATION).apply {
                putExtra(KEY_IDENTITY, identity)
            })
        }
    }

    private fun handleSyncContacts() {
        if (!indexPrefs.contactSyncEnabled || !hasContactsReadPermission())
            return

        val syncResults = indexService.syncContacts(AndroidContactsAccessor(applicationContext))
        if (syncResults.isNotEmpty()) {
            applicationContext.sendBroadcast(Intent(Contacts.CONTACTS_CHANGED))
            val grouped = syncResults.groupBy { it.action }
            val createdCount = grouped[IndexSyncAction.CREATE]?.size ?: 0
            val updatedCount = grouped[IndexSyncAction.UPDATE]?.size ?: 0
            info("ContactSync completed! Created: $createdCount, Updated: $updatedCount")
        }
    }

}
