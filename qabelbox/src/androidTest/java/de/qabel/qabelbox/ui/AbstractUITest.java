package de.qabel.qabelbox.ui;

import android.content.Intent;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;

/**
 * Created by dax_2 on 13.05.2016.
 */
public class AbstractUITest {
    @Rule
    public IntentsTestRule<MainActivity> mActivityTestRule =
            new MainActivityWithoutFilesFragmentTestRule(false);
    protected MainActivity mActivity;
    protected UIBoxHelper mBoxHelper;
    protected Identity identity;
    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;

    @After
    public void cleanUp() {
        if (wakeLock != null) {
            wakeLock.release();
        }
        if (mSystemAnimations != null) {
            mSystemAnimations.enableAll();
        }
        mBoxHelper.unbindService(QabelBoxApplication.getInstance());
    }

    @Before
    public void setUp() throws Exception {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.createTokenIfNeeded(false);
        mBoxHelper.removeAllIdentities();
        identity = mBoxHelper.addIdentity("spoon123");

    }

    protected void launchActivity(@Nullable Intent intent) {
        mActivity = mActivityTestRule.launchActivity(intent);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }
}
