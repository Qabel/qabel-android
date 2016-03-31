package de.qabel.qabelbox.ui;


import android.os.PowerManager;
import android.support.design.internal.NavigationMenuItemView;
import android.support.test.rule.ActivityTestRule;
import android.widget.SeekBar;
import com.squareup.spoon.Spoon;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.ui.action.QabelViewAction;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.closeDrawer;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.*;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateIdentityUITest {

    @Rule
    public ActivityTestRule<CreateIdentityActivity> mActivityTestRule = new ActivityTestRule<>(CreateIdentityActivity.class, false, true);

    private CreateIdentityActivity mActivity;

    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;
    private UIBoxHelper mBoxHelper;

    @After
    public void cleanUp() {

        wakeLock.release();
        mSystemAnimations.enableAll();
        mBoxHelper.unbindService(QabelBoxApplication.getInstance());
    }


    @Before
    public void setUp() throws IOException, QblStorageException {

        mActivity = mActivityTestRule.getActivity();
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.createTokenIfNeeded(false);


        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }


    public void clearIdentities() {
        //clear all identities
        LocalQabelService service = QabelBoxApplication.getInstance().getService();
        Identities identities = service.getIdentities();
        for (Identity identity : identities.getIdentities()) {
            service.deleteIdentity(identity);
        }
        mBoxHelper.removeAllIdentities();


    }

    public void openDrawerWithIdentity(String withIdentity) {
        openDrawer(R.id.drawer_layout);
        onView(allOf(withText(withIdentity), withParent(withId(R.id.select_identity_layout)))).check(matches(isDisplayed())).perform(click());
        UITestHelper.sleep(2000);
    }

    @Test
    public void addIdentity0Test() throws Throwable {
        clearIdentities();
    }

    @Test
    public void addIdentity1Test() throws Throwable {
        clearIdentities();
        UITestHelper.sleep(500);
        String identity = "spoon1";
        String identity2 = "spoon2";
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "start");

        pressBack();
        onView(withText(String.format(mActivity.getString(R.string.message_step_is_needed_or_close_app), R.string.identity)));
        onView(withText(R.string.no)).perform(click());

        createIdentity(identity);
        openDrawerWithIdentity(identity);
        //go to add identity, enter no data and go back
        onView(withText(R.string.add_identity)).check(matches(isDisplayed())).perform(click());
        pressBack();
        onView(withText(R.string.headline_files)).check(matches(isDisplayed()));

        // Wait for back is performed
        UITestHelper.sleep(500);

        //create spoon 2 identity
        openDrawerWithIdentity(identity);

        onView(withText(R.string.add_identity)).check(matches(isDisplayed())).perform(click());
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "spoon1");
        //go to add identity, enter no data and go back
        createIdentity(identity2);
        UITestHelper.sleep(500);
        //check if 2 identities displayed
        closeDrawer(R.id.drawer_layout);
        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
        UITestHelper.sleep(500);
        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText(identity))).check(matches(isDisplayed()));
        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText(identity2))).check(matches(isDisplayed()));

        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "spoon1_2");
        closeDrawer(R.id.drawer_layout);
    }

    private void createIdentity(String identity) throws Throwable {
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
        UITestHelper.sleep(500);
        onView(withText(R.string.next)).perform(click());
        UITestHelper.sleep(500);
    }

    private void createIdentityPerformSetSecurityLevel() {
        onView(withClassName(equalTo(SeekBar.class.getName()))).perform(QabelViewAction.setProgress(2));
        onView(allOf(withClassName(endsWith("SeekBar")))).check(matches(QabelMatcher.withProgress(2)));
        onView(withText(R.string.next)).perform(click());
        UITestHelper.sleep(500);
    }

    private void createIdentityPerformConfirm() {
        onView(withText(R.string.create_identity_final)).check(matches(isDisplayed()));
        onView(withText(R.string.finish)).perform(click());
        UITestHelper.sleep(10000);
        onView(withText(R.string.headline_files)).check(matches(isDisplayed()));
        UITestHelper.sleep(500);
    }
}

