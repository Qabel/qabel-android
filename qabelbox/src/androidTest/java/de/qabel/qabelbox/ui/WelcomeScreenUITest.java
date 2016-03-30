package de.qabel.qabelbox.ui;


import android.os.PowerManager.WakeLock;
import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;
import com.squareup.spoon.Spoon;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.activities.WelcomeScreenActivity;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertTrue;


/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WelcomeScreenUITest {
    @Rule
    public ActivityTestRule<WelcomeScreenActivity> mActivityTestRule = new ActivityTestRule<>(WelcomeScreenActivity.class, false, true);

    private WelcomeScreenActivity mActivity;

    private WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;
    private AppPreference prefs;

    public WelcomeScreenUITest() throws IOException {
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
        prefs = new AppPreference(mActivity);

    }


    @Test
    public void testWelcomeScreenSlide() {
        prefs.setWelcomeScreenShownAt(0);
        int pagerId = id.pager;
        onView(withId(id.btn_show_sources)).check(matches(isDisplayed()));
        onView(withText(string.headline_welcome_screen1)).check(matches(isDisplayed()));

        Spoon.screenshot(mActivity, "welcome1");
        onView(withId(pagerId)).perform(swipeLeft());
        onView(withText(string.headline_welcome_screen2)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "welcome2");

        onView(withId(pagerId)).perform(swipeLeft());
        onView(withText(string.headline_welcome_screen3)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "welcome3");

        onView(withId(pagerId)).perform(swipeLeft());
        onView(withText(string.headline_welcome_screen4)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "welcome4");

        onView(withId(pagerId)).perform(swipeLeft());


    }

    @Test
    public void testWelcomeScreenDisclaimer() {
        prefs.setWelcomeScreenShownAt(0);
        int pagerId = id.pager;
        onView(withId(pagerId)).perform(swipeLeft());
        onView(withId(pagerId)).perform(swipeLeft());
        onView(withId(pagerId)).perform(swipeLeft());
        onView(withId(pagerId)).perform(swipeLeft());

        Spoon.screenshot(mActivity, "disclaimer_main");
        //click privacy
        onView(withId(id.btn_show_privacy)).check(matches(isDisplayed())).perform(click());
        Spoon.screenshot(mActivity, "privacy");
        pressBack();
        onView(withText(string.message_welcome_disclaimer_headline)).check(matches(isDisplayed()));

        //click qabel
        onView(withId(id.layout_show_qabel)).perform(ViewActions.scrollTo()).check(matches(isDisplayed())).perform(click());
        Spoon.screenshot(mActivity, "qabl");
        pressBack();
        onView(withText(string.message_welcome_disclaimer_headline)).check(matches(isDisplayed()));

        //go back
        onView(withText(string.btn_welcome_back)).perform(click());
        onView(withText(string.headline_welcome_screen4)).check(matches(isDisplayed()));
        onView(withText(string.btn_welcome_skip)).check(matches(isDisplayed())).perform(click());


        //click legal
        onView(withId(id.btn_show_legal)).check(matches(isDisplayed())).perform(click());
        Spoon.screenshot(mActivity, "legal");
        pressBack();
        onView(withText(string.message_welcome_disclaimer_headline)).check(matches(isDisplayed()));


        //click without accept cbs
        onView(withText(string.btn_welcome_accept)).perform(click());
        onView(withText(string.message_welcome_disclaimer_headline)).check(matches(isDisplayed()));

        //accept cb
        onView(withText(string.cb_welcome_disclaimer_legal_note_unchecked)).check(matches(isDisplayed()));
        onView(withId(id.cb_welcome_legal)).perform(click());
        onView(withText(string.cb_welcome_disclaimer_legal_note_checked)).check(matches(isDisplayed()));

        onView(withText(string.cb_welcome_disclaimer_privacy_note_unchecked)).check(matches(isDisplayed()));
        onView(withId(id.cb_welcome_privacy)).perform(click());
        onView(withText(string.cb_welcome_disclaimer_privacy_note_checked)).check(matches(isDisplayed()));
        onView(withText(string.cb_welcome_disclaimer_legal_note_checked)).check(matches(isDisplayed()));

        //click accept
        onView(withText(string.btn_welcome_accept)).perform(click());
        assertTrue(prefs.getWelcomeScreenShownAt() > 0);

        //check if create box account in foreground
        QabelMatcher.matchToolbarTitle(mActivity.getString(string.headline_create_box_account))
                .check(matches(isDisplayed()));

    }
}
