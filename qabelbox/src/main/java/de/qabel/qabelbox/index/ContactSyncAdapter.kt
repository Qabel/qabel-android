package de.qabel.qabelbox.index

import android.accounts.Account
import android.accounts.AccountManager
import android.content.*
import android.os.Bundle
import de.qabel.core.index.IndexService
import de.qabel.core.index.IndexSyncAction
import de.qabel.core.index.server.ExternalContactsAccessor
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.helper.AccountHelper
import de.qabel.qabelbox.index.preferences.AndroidIndexPreferences
import de.qabel.qabelbox.index.preferences.IndexPreferences
import de.qabel.qabelbox.permissions.DataPermissionsAdapter
import de.qabel.qabelbox.permissions.hasContactsReadPermission
import javax.inject.Inject

open class ContactSyncAdapter : AbstractThreadedSyncAdapter, QabelLog, DataPermissionsAdapter {

    override val permissionContext: Context
        get() = context

    lateinit internal var mContentResolver: ContentResolver
    @Inject lateinit internal var context: Context
    @Inject lateinit internal var indexService: IndexService
    @Inject lateinit internal var indexPreferences: IndexPreferences
    lateinit internal var contactsAccessor: ExternalContactsAccessor

    object Manager : QabelLog {
        val AUTHORITY = BuildConfig.INDEX_AUTHORITY
        // every day
        var SYNC_INTERVAL = 60 * 60 * 24.toLong()

        fun configureSync(context: Context) {
            val accountManager = AccountManager.get(context)
            accountManager.addAccountExplicitly(AccountHelper.DEFAULT_ACCOUNT, null, null)
            configurePeriodicPolling(context)
        }

        fun startOnDemandSyncAdapter() {
            val settingsBundle = Bundle()
            settingsBundle.putBoolean(
                    ContentResolver.SYNC_EXTRAS_MANUAL, true)
            settingsBundle.putBoolean(
                    ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            ContentResolver.requestSync(AccountHelper.DEFAULT_ACCOUNT, AUTHORITY, settingsBundle)
        }

        private fun configurePeriodicPolling(context: Context) {
            info("Configure contact sync. interval $SYNC_INTERVAL")
            ContentResolver.setSyncAutomatically(AccountHelper.DEFAULT_ACCOUNT, AUTHORITY, true)
            for (sync in getPeriodicSync()) {
                ContentResolver.removePeriodicSync(sync.account, sync.authority, sync.extras)
            }
            if (SYNC_INTERVAL > 0 && AndroidIndexPreferences(context).contactSyncEnabled) {
                ContentResolver.addPeriodicSync(
                        AccountHelper.DEFAULT_ACCOUNT,
                        AUTHORITY,
                        Bundle.EMPTY,
                        SYNC_INTERVAL)
            }

        }

        fun getPeriodicSync(): List<PeriodicSync> {
            return ContentResolver.getPeriodicSyncs(AccountHelper.DEFAULT_ACCOUNT, AUTHORITY)
        }
    }

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
        QabelBoxApplication.getApplicationComponent(context).indexComponent().inject(this)
        contactsAccessor = AndroidContactsAccessor(context)
        info("ContactSyncAdapter initialized")
    }

    override fun onPerformSync(
            account: Account,
            extras: Bundle,
            authority: String,
            provider: ContentProviderClient,
            syncResult: SyncResult) {
        if (indexPreferences.contactSyncEnabled && hasContactsReadPermission()) {
            try {
                info("Starting contact sync")
                val syncResults = indexService.syncContacts(AndroidContactsAccessor(context))
                if (syncResults.isNotEmpty()) {
                    context.sendBroadcast(Intent(QblBroadcastConstants.Contacts.CONTACTS_CHANGED))
                    val grouped = syncResults.groupBy { it.action }
                    val createdCount = grouped[IndexSyncAction.CREATE]?.size ?: 0
                    val updatedCount = grouped[IndexSyncAction.UPDATE]?.size ?: 0
                    info("ContactSync completed! Created: $createdCount, Updated: $updatedCount")
                }
            } catch(ex: Throwable) {
                warn("Error on syncing contacts", ex)
            }
        } else {
            info("Ignoring contact sync. Is disabled or permissions missing.")
        }
    }
}
