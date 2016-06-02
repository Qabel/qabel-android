package de.qabel.qabelbox.ui;

import android.os.PowerManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static de.qabel.qabelbox.ui.action.QabelViewAction.setText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class CreateIdentityUITest {

    @Rule
    public ActivityTestRule<CreateIdentityActivity> mActivityTestRule =
            new ActivityTestRule<>(CreateIdentityActivity.class, false, false);

    private CreateIdentityActivity mActivity;
    private UIBoxHelper helper;
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
    }


    @Before
    public void setUp() throws Exception {

        helper = new UIBoxHelper(InstrumentationRegistry.getTargetContext());
        helper.removeAllIdentities();


        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        URLs.setBaseAccountingURL(TestConstants.ACCOUNTING_URL);

        mActivity = mActivityTestRule.launchActivity(null);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }


    @Test
    public void testCreateIdentity() throws Throwable {
        String identity = "spoon2";
        createIdentityPerformEnterName(identity);
        defaultSecurityLevel();
        createIdentityPerformConfirm();
        assertThat(helper.getCurrentIdentity().getAlias(), equalTo(identity));
    }

    private void createIdentityPerformEnterName(String identity) throws Throwable {
        onView(withText(R.string.create_identity_create)).check(matches(isDisplayed())).perform(click());
        onView(allOf(withClassName(endsWith("EditTextFont")))).perform(setText(identity), pressImeActionButton());
        onView(withText(R.string.create_identity_enter_name)).check(matches(isDisplayed()));
        closeSoftKeyboard();

        UITestHelper.screenShot(UITestHelper.getCurrentActivity(mActivity), "input");
        onView(withText(R.string.next)).perform(click());
    }

    private void defaultSecurityLevel() {
        onView(withText(R.string.next)).perform(click());
    }

    private void createIdentityPerformConfirm() {
        onView(withText(R.string.create_identity_final)).check(matches(isDisplayed()));
        onView(withText(R.string.finish)).perform(click());
        onView(withText(R.string.headline_files)).check(matches(isDisplayed()));
        UITestHelper.sleep(500);
    }
}

