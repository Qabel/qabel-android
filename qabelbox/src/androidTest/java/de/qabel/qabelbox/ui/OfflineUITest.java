package de.qabel.qabelbox.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import com.squareup.spoon.Spoon;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class OfflineUITest {


    @Rule
    public IntentsTestRule<MainActivity> mActivityTestRule = new IntentsTestRule<>(MainActivity.class, false, true);

    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;

    private Identity testIdentity;


    @Before
    public void setUp() {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        mActivity = mActivityTestRule.getActivity();
        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.createTokenIfNeeded(false);

        mBoxHelper.removeAllIdentities();
        testIdentity = mBoxHelper.addIdentity("spoon");

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

    private void setFlightModeEnabled(Context context, boolean enabled) {
        setMobileDataEnabled(context, !enabled);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setMobileDataEnabled(Context context, boolean state) {
        try {
            final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testOfflineIndicator() {
        Spoon.screenshot(mActivity, "startup");

        //Test the offline functionslity in the filebrowser
        QabelMatcher.matchToolbarTitle(mActivity.getString(R.string.headline_files))
                .check(matches(isDisplayed()));
        onView(withId(R.id.files_list)).check(matches(isDisplayed()));

        try {
            setFlightModeEnabled(mActivity, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        UITestHelper.sleep(10000);

        onView(withText(R.string.server_access_failed_or_invalid_check_internet_connection)).check(matches(isDisplayed()));
        UITestHelper.sleep(2000);
        try {
            setFlightModeEnabled(mActivity, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        onView(withText(R.string.server_access_failed_or_invalid_check_internet_connection)).check(doesNotExist());
    }

}
