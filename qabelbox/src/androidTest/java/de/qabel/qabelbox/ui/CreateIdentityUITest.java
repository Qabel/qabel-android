package de.qabel.qabelbox.ui;

/**
 * Created by danny on 05.01.2016.
 */

import android.os.PowerManager;
import android.support.design.internal.NavigationMenuItemView;
import android.support.test.rule.ActivityTestRule;
import android.widget.SeekBar;

import com.squareup.spoon.Spoon;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.action.QabelViewAction;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.closeDrawer;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateIdentityUITest extends UIBoxHelper {

    @Rule
    public ActivityTestRule<CreateIdentityActivity> mActivityTestRule = new ActivityTestRule<>(CreateIdentityActivity.class, false, true);

    private CreateIdentityActivity mActivity;

    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;

    String identity = "spoon1";
    String identity2 = "spoon2";

    @After
    public void cleanUp() {

        wakeLock.release();
        mSystemAnimations.enableAll();
        unbindService(QabelBoxApplication.getInstance());
    }


    @Before
    public void setUp() throws IOException, QblStorageException {

        mActivity = mActivityTestRule.getActivity();

        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        URLs.setBaseAccountingURL(TestConstants.ACCOUNTING_URL);

        new AppPreference(mActivity).setToken(TestConstants.TOKEN);
        bindService(QabelBoxApplication.getInstance());

        removeAllIdentities();
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }


    @Test
    public void addIdentity0Test() throws Throwable {
        removeAllIdentities();
    }

    @Test
    public void addIdentity1Test() throws Throwable {
        removeAllIdentities();

        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "start");

        pressBack();
        onView(withText(String.format(mActivity.getString(R.string.message_step_is_needed_or_close_app), R.string.identity)));
        onView(withText(R.string.no)).perform(click());

        testCreateIdentity(identity);

        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.textViewSelectedIdentity)).check(matches(isDisplayed())).perform(click());

        //go to add identity, enter no data and go back
        onView(withText(R.string.add_identity)).check(matches(isDisplayed())).perform(click());
        pressBack();
        onView(withText(R.string.headline_files)).check(matches(isDisplayed()));

        //create spoon 2 identity
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "spoon1");
        //go to add identity, enter no data and go back
        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.textViewSelectedIdentity)).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.add_identity)).check(matches(isDisplayed())).perform(click());
        testCreateIdentity(identity2);
        //check if 2 identities displayed
        closeDrawer(R.id.drawer_layout);
        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText(identity))).check(matches(isDisplayed()));
        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText(identity2))).check(matches(isDisplayed()));

        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "spoon1_2");
        closeDrawer(R.id.drawer_layout);
    }

    @Test
    public void logoutTest() {
        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.accountName))
                .check(matches(isDisplayed()))
                .perform(click());
        onView(withText("logout"))
                .check(matches(isDisplayed()))
                .perform(click());
        onView(withId(R.id.bt_login)).check(matches(isDisplayed()));
        assertHasNoCredentials();
    }

    private void assertHasNoCredentials() {
        AppPreference appPrefs = new AppPreference(mActivity);
        assertNull(appPrefs.getToken());
        assertNull(appPrefs.getAccountName());
    }

    private void testCreateIdentity(String identity) throws Throwable {
        createIdentityPerformEnterName(identity);
        createIdentityPerformSetSecurityLevel();
        createIdentityPerformConfirm();
    }

    private void createIdentityPerformEnterName(String identity) throws Throwable {
        onView(withText(R.string.create_identity_create)).check(matches(isDisplayed())).perform(click());
        onView(allOf(withClassName(endsWith("EditTextFont")))).perform(typeText(identity), pressImeActionButton());
        onView(withText(R.string.create_identity_enter_name)).check(matches(isDisplayed()));
        closeSoftKeyboard();

        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "input");
        onView(withText(R.string.next)).perform(click());
        UITestHelper.sleep(500);
    }

    private void createIdentityPerformSetSecurityLevel() {
        onView(withClassName(Matchers.equalTo(SeekBar.class.getName()))).perform(QabelViewAction.setProgress(2));
        onView(allOf(withClassName(endsWith("SeekBar")))).check(matches(QabelMatcher.withProgress(2)));
        onView(withText(R.string.next)).perform(click());
        UITestHelper.sleep(500);
    }

    private void createIdentityPerformConfirm() {
        onView(withText(R.string.create_identity_final)).check(matches(isDisplayed()));
        onView(withText(R.string.finish)).perform(click());
        onView(withText(R.string.headline_files)).check(matches(isDisplayed()));
        UITestHelper.sleep(500);
    }
}

