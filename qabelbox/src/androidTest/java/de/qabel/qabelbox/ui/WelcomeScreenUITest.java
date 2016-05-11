package de.qabel.qabelbox.ui;

import android.os.PowerManager;
import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;

import com.squareup.spoon.Spoon;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.WelcomeScreenActivity;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.matcher.ToolbarMatcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertTrue;


public class WelcomeScreenUITest {

    @Rule
    public ActivityTestRule<WelcomeScreenActivity> mActivityTestRule =
            new ActivityTestRule<WelcomeScreenActivity>(WelcomeScreenActivity.class, false, true) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();
            UIBoxHelper mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
            mBoxHelper.bindService(QabelBoxApplication.getInstance());
            mBoxHelper.removeAllIdentities();
            mBoxHelper.addIdentityWithoutVolume("user1");
            prefs = new AppPreference(QabelBoxApplication.getInstance());
            prefs.setWelcomeScreenShownAt(0);
        }
    };

    private WelcomeScreenActivity mActivity;

    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;
    private AppPreference prefs;

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
    public void testWelcomeScreenSlide() {
        int pagerId = R.id.pager;
        onView(withId(R.id.btn_show_sources)).check(matches(isDisplayed()));
        onView(withText(R.string.headline_welcome_screen1)).check(matches(isDisplayed()));

        Spoon.screenshot(mActivity, "welcome1");
        onView(withId(pagerId)).perform(swipeLeft());
        onView(withText(R.string.headline_welcome_screen2)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "welcome2");

        onView(withId(pagerId)).perform(swipeLeft());
        onView(withText(R.string.headline_welcome_screen3)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "welcome3");

        onView(withId(pagerId)).perform(swipeLeft());
        onView(withText(R.string.headline_welcome_screen4)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "welcome4");

        onView(withId(pagerId)).perform(swipeLeft());

        checkDisclaimer();
    }

    private void checkDisclaimer() {
        Spoon.screenshot(mActivity, "disclaimer_main");

        //click privacy
        UITestHelper.waitForView(R.id.btn_show_privacy,1500).check(matches(isDisplayed())).perform(click());
        Spoon.screenshot(mActivity, "privacy");
        pressBack();
        onView(withText(R.string.message_welcome_disclaimer_headline)).check(matches(isDisplayed()));

        //click qabel
        onView(withId(R.id.layout_show_qabel)).perform(ViewActions.scrollTo()).check(matches(isDisplayed())).perform(click());
        Spoon.screenshot(mActivity, "qabl");
        pressBack();
        onView(withText(R.string.message_welcome_disclaimer_headline)).check(matches(isDisplayed()));

        //go back
        onView(withText(R.string.btn_welcome_back)).perform(click());
        onView(withText(R.string.headline_welcome_screen4)).check(matches(isDisplayed()));
        onView(withText(R.string.btn_welcome_skip)).check(matches(isDisplayed())).perform(click());


        //click legal
        onView(withId(R.id.btn_show_legal)).check(matches(isDisplayed())).perform(click());
        Spoon.screenshot(mActivity, "legal");
        pressBack();
        onView(withText(R.string.message_welcome_disclaimer_headline)).check(matches(isDisplayed()));


        //click without accept cbs
        onView(withText(R.string.btn_welcome_accept)).perform(click());
        onView(withText(R.string.message_welcome_disclaimer_headline)).check(matches(isDisplayed()));

        //accept cb
        onView(withText(R.string.cb_welcome_disclaimer_legal_note_unchecked)).check(matches(isDisplayed()));
        onView(withId(R.id.cb_welcome_legal)).perform(click());
        onView(withText(R.string.cb_welcome_disclaimer_legal_note_checked)).check(matches(isDisplayed()));

        onView(withText(R.string.cb_welcome_disclaimer_privacy_note_unchecked)).check(matches(isDisplayed()));
        onView(withId(R.id.cb_welcome_privacy)).perform(click());
        onView(withText(R.string.cb_welcome_disclaimer_privacy_note_checked)).check(matches(isDisplayed()));
        onView(withText(R.string.cb_welcome_disclaimer_legal_note_checked)).check(matches(isDisplayed()));

        //click accept
        onView(withText(R.string.btn_welcome_accept)).perform(click());
        assertTrue(prefs.getWelcomeScreenShownAt() > 0);

        //check if create box account in foreground
        ToolbarMatcher.matchToolbarTitle(mActivity.getString(R.string.headline_create_box_account))
                .check(matches(isDisplayed()));

    }
}
