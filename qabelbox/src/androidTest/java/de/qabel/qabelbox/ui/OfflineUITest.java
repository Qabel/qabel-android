package de.qabel.qabelbox.ui;

import android.app.Activity;
import android.os.PowerManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;

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
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class OfflineUITest {

    private static final String ALERT_DIALOG_WATCHER_NAME = "WATCH_CONNECTION_LOST";

    @Rule
    public ActivityTestRule<MainActivity> rule = new ActivityTestRule<MainActivity>(MainActivity.class);

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

    public OfflineUITest() {
        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.createTokenIfNeeded(false);

        mBoxHelper.removeAllIdentities();
        testIdentity = mBoxHelper.addIdentity("spoon");
    }

    @Before
    public void setUp() {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        mActivity = rule.getActivity();
        connectivityManager = new MockConnectivityManager(mActivity);
        mActivity.setConnectivityManager(connectivityManager);

        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }

    @After
    public void tearDown() {
        mBoxHelper.deleteIdentity(testIdentity);
        wakeLock.release();
        mSystemAnimations.enableAll();
        mBoxHelper.unbindService(QabelBoxApplication.getInstance());
    }


    @Test
    public void testOfflineIndicator() throws UiObjectNotFoundException {
        final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        Spoon.screenshot(mActivity, "startup");

   /*     UiWatcher watcher = new UiWatcher() {
            @Override
            public boolean checkForCondition() {
                System.out.println("WATCHER CHECKED");
                UiObject dialog = uiDevice.findObject(new UiSelector().className(AlertDialog.class.getName()));//.text(mActivity.getText(R.string.server_access_failed_or_invalid_check_internet_connection).toString()));
                if (dialog.exists()) {
                    System.out.println("WATCHER TRIGGERED");
                    connectivityManager.setConnected(true);
                    return dialog.waitUntilGone(2000);
                }
                return false;
            }
        };

        uiDevice.registerWatcher(ALERT_DIALOG_WATCHER_NAME, watcher);
*/
        connectivityManager.setConnected(false);

        onView(withText(R.string.no_connection)).check(matches(isDisplayed()));
        /*
        uiDevice.runWatchers();

        //Wait for Dialog
        assertTrue(uiDevice.waitForWindowUpdate(null, 5000));
        //Wait for dismiss dialog
        assertTrue(uiDevice.waitForWindowUpdate(null, 5000));
        assertTrue(uiDevice.hasWatcherTriggered(ALERT_DIALOG_WATCHER_NAME));
        */
    }

}
