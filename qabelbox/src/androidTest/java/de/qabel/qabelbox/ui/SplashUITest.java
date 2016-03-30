package de.qabel.qabelbox.ui;


import android.os.PowerManager.WakeLock;
import android.support.test.rule.ActivityTestRule;
import com.squareup.spoon.Spoon;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.activities.SplashActivity;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SplashUITest {
    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(SplashActivity.class, false, true);

    private SplashActivity mActivity;

    private WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;

    public SplashUITest() throws IOException {
        new AppPreference(QabelBoxApplication.getInstance()).setWelcomeScreenShownAt(1);
    }

    @After
    public void cleanUp() {
        wakeLock.release();
        mSystemAnimations.enableAll();
    }

    @Before
    public void setUp() throws IOException, QblStorageException {
        mActivity = mActivityTestRule.getActivity();

        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }


    @Test
    public void testShowSplashScreen() {
        onView(withText(string.splash_footer_text)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "splashscreen");


    }

}
