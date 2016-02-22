package de.qabel.qabelbox.ui;

/**
 * Created by danny on 05.01.2016.
 */

import android.content.Context;
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

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.ui.action.QabelViewAction;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.Is.is;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateIdentityTest {

    @Rule
    public ActivityTestRule<CreateIdentityActivity> mActivityTestRule = new ActivityTestRule<>(CreateIdentityActivity.class, false, true);

    private CreateIdentityActivity mActivity;

    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;

    @After
    public void cleanUp() {

        wakeLock.release();
        mSystemAnimations.enableAll();
        clearIdentities();
    }

    public void configureTestServer() {
        Context applicationContext = QabelBoxApplication.getInstance().getApplicationContext();
        AppPreference pref = new AppPreference(applicationContext);
        pref.setToken(applicationContext.getString(R.string.accountingServerInactiveToken));
        URLs.setBaseAccountingURL(applicationContext.getString(R.string.accountingServer));
        URLs.setBaseBlockURL(applicationContext.getString(R.string.testBlockServer));

    }

    @Before
    public void setUp() throws IOException, QblStorageException {
        configureTestServer();
        mActivity = mActivityTestRule.getActivity();
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }


    public void clearIdentities() {
        Context applicationContext = QabelBoxApplication.getInstance().getApplicationContext();
        new AppPreference(applicationContext)
                .setToken(applicationContext.getString(R.string.accountingServerInactiveToken));

        //clear all identities
        LocalQabelService service = QabelBoxApplication.getInstance().getService();
        Identities identities = service.getIdentities();
        for (Identity identity : identities.getIdentities()) {
            service.deleteIdentity(identity);
        }


    }

    public void openDrawer(String withIdentity) {
        onView(withId(R.id.drawer_layout)).perform(QabelViewAction.actionOpenDrawer());
        UITestHelper.sleep(500);
        onView(allOf(withText(withIdentity), withParent(withId(R.id.select_identity_layout)))).check(matches(isDisplayed())).perform(click());
        UITestHelper.sleep(2000);

    }




    @Test
    public void addIdentity1Test() throws Throwable {
        clearIdentities();
        UITestHelper.sleep(500);
        String identity = "spoon1";
        String identity2 = "spoon2";
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "start");

        createIdentity(identity);
        openDrawer(identity);
        //go to add identity, enter no data and go back
        onView(withText(R.string.add_identity)).check(matches(isDisplayed())).perform(click());
        pressBack();
        onView(withText(R.string.headline_files)).check(matches(isDisplayed()));

        //create spoon 2 identity

        openDrawer(identity);
        onView(withText(R.string.add_identity)).check(matches(isDisplayed())).perform(click());
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "spoon1");
        //go to add identity, enter no data and go back
        createIdentity(identity2);
        UITestHelper.sleep(500);
        onView(allOf(withText(identity), withParent(withId(R.id.select_identity_layout)))).check(matches(isDisplayed()));
        //check if 2 identities displayer
        //create spoon 2 identity
        onView(withId(R.id.drawer_layout)).check(matches(isDisplayed())).perform(QabelViewAction.actionCloseDrawer());
        UITestHelper.sleep(500);
        onView(withId(R.id.drawer_layout)).check(matches(isDisplayed())).perform(QabelViewAction.actionOpenDrawer());
        UITestHelper.sleep(1000);
        onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText(identity2))).check(matches(isDisplayed()));

        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "spoon1_2");
        onView(withId(R.id.drawer_layout)).perform(QabelViewAction.actionCloseDrawer());
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

