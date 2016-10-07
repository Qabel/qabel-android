package de.qabel.qabelbox.account

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.qabel.qabelbox.*
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.storage.model.BoxQuota
import de.qabel.qabelbox.storage.server.MockBlockServer
import de.qabel.qabelbox.util.BoxTestHelper
import junit.framework.Assert.*
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class AccountManagerTest {

    private lateinit var accountManager: AccountManager
    private lateinit var appPreferences: AppPreference

    private val GB_100: Long = 107374182400L
    private val KB_1: Long = 1024L

    var statusCodes = -1
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            statusCodes = intent.getIntExtra(QblBroadcastConstants.STATUS_CODE_PARAM, 0)
        }
    }

    @Before
    fun setUp() {
        val testHelper = BoxTestHelper(RuntimeEnvironment.application as QabelBoxApplication)
        accountManager = testHelper.accountManager
        appPreferences = testHelper.appPreferences.apply {
            accountName = "testUser"
            accountEMail = "test@user.de"
            token = TestConstants.TOKEN
            boxQuota = null
        }

        statusCodes = -1
        RuntimeEnvironment.application.registerReceiver(receiver,
                IntentFilter(QblBroadcastConstants.Account.ACCOUNT_CHANGED))
    }

    @Test
    fun testLogout() {
        accountManager.logout()
        with(appPreferences) {
            assertNull(token)
            assertNotNull(accountName)
            assertNotNull(accountEMail)
        }
    }

    @Test
    fun testRefreshQuota() {
        var quota = accountManager.boxQuota
        //Initial values
        quota.size eq 0
        quota.quota eq -1

        waitFor({ statusCodes > 0 }, "Error waiting for quota response!")

        assertEquals(AccountStatusCodes.QUOTA_UPDATED, statusCodes)
        quota = accountManager.boxQuota

        MockBlockServer.SIZE eq quota.size
        MockBlockServer.QUOTA eq quota.quota
    }

    @Test
    fun testRefreshUpdatedQuota() {
        val boxQuota = BoxQuota().apply {
            size = MockBlockServer.SIZE
            quota = MockBlockServer.QUOTA
        }
        appPreferences.boxQuota = boxQuota

        //100Gb
        MockBlockServer.QUOTA = GB_100
        MockBlockServer.SIZE = KB_1

        //Old values
        val outDatedQuota = accountManager.boxQuota
        boxQuota eq outDatedQuota

        waitFor({ statusCodes > 0 }, "Error waiting for changed quota response!")

        val updatedQuota = accountManager.boxQuota

        KB_1 eq updatedQuota.size
        GB_100 eq updatedQuota.quota

        "100 GB" eq FileUtils.byteCountToDisplaySize(updatedQuota.quota)
        "1 KB" eq FileUtils.byteCountToDisplaySize(updatedQuota.size)
    }

    fun waitFor(check: () -> Boolean, message: String, timeout: Long = 200) {
        val maximumTime = System.currentTimeMillis() + timeout
        val pollInterval = 100L
        while (System.currentTimeMillis() < maximumTime) {
            if (check()) {
                return
            }
            Thread.sleep(pollInterval)
        }
        fail(message)
    }
}
