package de.qabel.qabelbox.index

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.IBinder
import de.qabel.core.config.Identity
import de.qabel.core.config.VerificationStatus
import de.qabel.core.index.IndexService
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.QblBroadcastConstants.Identities.*
import de.qabel.qabelbox.QblBroadcastConstants.Index
import de.qabel.qabelbox.QblBroadcastConstants.Index.*
import de.qabel.qabelbox.index.preferences.IndexPreferences
import de.qabel.qabelbox.permissions.DataPermissionsAdapter
import javax.inject.Inject

class AndroidIndexSyncService() : IntentService(AndroidIndexSyncService::class.java.simpleName),
        QabelLog, DataPermissionsAdapter {

    override val permissionContext: Context by lazy { applicationContext }
    private val contactSyncAdapter: ContactSyncAdapter by lazy { ContactSyncAdapter(applicationContext, true) }
    @Inject lateinit var indexService: IndexService

    companion object {
        private fun start(context: Context, action: String) {
            context.startService(Intent(action, null, context.applicationContext, AndroidIndexSyncService::class.java))
        }

        fun startSyncVerifications(context: Context) =
                start(context, Index.SYNC_VERIFICATIONS)
    }

    override fun onCreate() {
        super.onCreate()
        QabelBoxApplication.getApplicationComponent(applicationContext).indexComponent().inject(this)
        info("Service initialized!")
    }

    override fun onBind(intent: Intent?): IBinder =
            contactSyncAdapter.syncAdapterBinder

    private fun Intent.affectedIdentity(): Identity = getSerializableExtra(KEY_IDENTITY) as Identity
    private fun Intent.outdatedIdentity(): Identity = getSerializableExtra(OLD_IDENTITY) as Identity

    override fun onHandleIntent(intent: Intent) {
        debug("IndexSync received intent with action ${intent.action}")
        try {
            when (intent.action) {
                IDENTITY_CHANGED -> handleIdentityChanged(intent)
                IDENTITY_CREATED -> handleIdentityCreated(intent)
                IDENTITY_REMOVED -> handleRemoveIdentity(intent)
                SYNC_VERIFICATIONS -> indexService.updateIdentityVerifications()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            error("Error syncing with index. Action: ${intent.action}", ex)
        }
    }

    private fun handleIdentityCreated(intent: Intent) {
        val identity = intent.affectedIdentity()
        if (identity.isUploadEnabled) {
            indexService.updateIdentity(identity)
            sendRequestIntentIfRequiresPhoneVerification(identity)
        }
    }

    private fun handleIdentityChanged(intent: Intent) {
        val identity = intent.affectedIdentity()
        val oldIdentity = intent.outdatedIdentity()
        if (identity.isUploadEnabled) {
            indexService.updateIdentity(identity, oldIdentity)
        } else {
            indexService.removeIdentity(identity)
        }
        sendRequestIntentIfRequiresPhoneVerification(identity)
    }

    private fun handleRemoveIdentity(intent: Intent) {
        indexService.removeIdentity(intent.affectedIdentity())
    }

    private fun sendRequestIntentIfRequiresPhoneVerification(identity: Identity) {
        if (identity.phoneStatus == VerificationStatus.NOT_VERIFIED) {
            sendBroadcast(Intent(REQUEST_VERIFICATION).apply {
                putExtra(KEY_IDENTITY, identity)
            })
        }
    }

}
