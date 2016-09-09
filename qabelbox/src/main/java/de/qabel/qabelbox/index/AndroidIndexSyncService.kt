package de.qabel.qabelbox.index

import android.app.IntentService
import android.content.Intent
import de.qabel.core.config.Identity
import de.qabel.core.config.VerificationStatus
import de.qabel.core.index.IndexInteractor
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.QblBroadcastConstants.Identities.*
import de.qabel.qabelbox.QblBroadcastConstants.Index.REQUEST_VERIFICATION
import de.qabel.qabelbox.QblBroadcastConstants.Index.SYNC_CONTACTS
import de.qabel.qabelbox.chat.services.AndroidChatService
import de.qabel.qabelbox.index.dagger.IndexModule
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import javax.inject.Inject

class AndroidIndexSyncService() : IntentService(AndroidChatService::class.java.simpleName), AnkoLogger {

    @Inject lateinit var indexInteractor: IndexInteractor

    override fun onCreate() {
        super.onCreate()
        QabelBoxApplication.getApplicationComponent(applicationContext).plus(IndexModule()).inject(this)
        info("Service initialized!")
    }

    private fun Intent.affectedIdentity(): Identity = getSerializableExtra(QblBroadcastConstants.Identities.KEY_IDENTITY) as Identity
    private fun Intent.outdatedIdentity(): Identity = getSerializableExtra(QblBroadcastConstants.Identities.OLD_IDENTITY) as Identity

    override fun onHandleIntent(intent: Intent) {
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
                TODO()
            }
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
