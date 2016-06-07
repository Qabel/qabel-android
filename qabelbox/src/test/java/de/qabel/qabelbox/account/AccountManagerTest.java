package de.qabel.qabelbox.account;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.QblBroadcastConstants;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.storage.model.BoxQuota;
import de.qabel.qabelbox.storage.server.MockBlockServer;
import de.qabel.qabelbox.util.BoxTestHelper;
import de.qabel.qabelbox.util.TestHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class AccountManagerTest {

    private AccountManager accountManager;
    private AppPreference appPreferences;

    @Before
    public void setUp() {
        BoxTestHelper testHelper = new BoxTestHelper((QabelBoxApplication) RuntimeEnvironment.application);
        accountManager = testHelper.getAccountManager();
        appPreferences = testHelper.getAppPreferences();
        appPreferences.setAccountName("testUser");
        appPreferences.setAccountEMail("test@user.de");
        appPreferences.setToken(TestConstants.TOKEN);
        appPreferences.setBoxQuota(null);
    }

    @Test
    public void testLogout() {
        accountManager.logout();
        assertNull(appPreferences.getToken());
        assertNotNull(appPreferences.getAccountName());
        assertNotNull(appPreferences.getAccountEMail());
    }


    @Test
    public void testRefreshQuota() throws Exception {
        int[] statusCodes = new int[]{0};
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int statusCode = intent.getIntExtra(QblBroadcastConstants.STATUS_CODE_PARAM, 0);
                statusCodes[0] = statusCode;
            }
        };
        RuntimeEnvironment.application.registerReceiver(receiver,
                new IntentFilter(QblBroadcastConstants.Account.ACCOUNT_CHANGED));

        BoxQuota quota = accountManager.getBoxQuota();

        assertEquals(0, quota.getSize());
        assertEquals(-1, quota.getQuota());

        TestHelper.waitUntil(() -> statusCodes[0] > 0, "Error waiting for quota");
        assertEquals(AccountStatusCodes.QUOTA_UPDATED, statusCodes[0]);
        quota = accountManager.getBoxQuota();

        assertEquals(MockBlockServer.SIZE, quota.getSize());
        assertEquals(MockBlockServer.QUOTA, quota.getQuota());
    }
}
