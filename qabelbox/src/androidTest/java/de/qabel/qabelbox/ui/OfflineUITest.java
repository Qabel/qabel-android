package de.qabel.qabelbox.ui;

import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import com.squareup.spoon.Spoon;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class OfflineUITest {

    @Rule
    public ActivityTestRule<MainActivity> rule = new ActivityTestRule<MainActivity>(MainActivity.class, true, false);

    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;

    private Identity testIdentity;

    private MockConnectivityManager connectivityManager;

    class MockConnectivityManager extends de.qabel.qabelbox.communication.connection.ConnectivityManager {

        private boolean connected = true;
        private Activity context;

        public MockConnectivityManager(Activity context) {
            super(context);
            this.context = context;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        public void setConnected(final boolean connected) {
            this.connected = connected;
            final ConnectivityListener listener = this.listener;
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (connected) {
                        listener.handleConnectionEtablished();
                    } else {
                        listener.handleConnectionLost();
                    }
                }
            });
        }
    }


    public void setupBeforeLaunch() {
        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.createTokenIfNeeded(false);

        mBoxHelper.removeAllIdentities();
        testIdentity = mBoxHelper.addIdentity("spoon");
        mSystemAnimations = new SystemAnimations(InstrumentationRegistry.getTargetContext());
        mSystemAnimations.disableAll();
    }

    @Before
    public void setUp() {
        setupBeforeLaunch();

        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);

        mActivity = rule.launchActivity(null);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        connectivityManager = new MockConnectivityManager(mActivity);
        mActivity.installConnectivityManager(connectivityManager);
        connectivityManager.setConnected(true);
    }

    @After
    public void tearDown() {
        mBoxHelper.deleteIdentity(testIdentity);
        wakeLock.release();
        mSystemAnimations.enableAll();
        mBoxHelper.unbindService(QabelBoxApplication.getInstance());
    }


    @Test
    public void testOfflineIndicator() {
        onView(withText(R.string.no_connection)).check(doesNotExist());
        connectivityManager.setConnected(false);
        onView(withText(R.string.no_connection)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "offlineIndicator");
    }

    @Test
    public void testOnline() {
        onView(withText(R.string.no_connection)).check(doesNotExist());
    }

}
