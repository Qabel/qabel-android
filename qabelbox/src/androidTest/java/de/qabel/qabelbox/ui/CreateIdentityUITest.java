package de.qabel.qabelbox.ui;

import android.os.Build;
import android.os.PowerManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.communication.URLs;
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
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static de.qabel.qabelbox.ui.action.QabelViewAction.setText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

public class CreateIdentityUITest {

    @Rule
    public ActivityTestRule<CreateIdentityActivity> mActivityTestRule =
            new ActivityTestRule<>(CreateIdentityActivity.class, false, false);

    private CreateIdentityActivity mActivity;
    private UIBoxHelper helper;
    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;

    private String name = "spoon2";
    private String phone = "+49 234567890";
    private String phoneFormatted = "+49 234 567890";
    private String mail = "mail@test.de";

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
        helper.setTestAccount();
        helper.removeAllIdentities();


        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        URLs.setBaseAccountingURL(TestConstants.ACCOUNTING_URL);

        mActivity = mActivityTestRule.launchActivity(null);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();

        //TODO TMP Fix option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                    "pm grant " + InstrumentationRegistry.getTargetContext().getPackageName()
                            + " android.permission.READ_PHONE_STATE");
        }
    }


    @Test
    public void testCreateIdentity() throws Throwable {
        createIdentityPerformEnterName();
        createIdentityEnterEmail();
        createIdentityEnterPhone();
        createIdentityPerformConfirm();
    }

    private void createIdentityPerformEnterName() throws Throwable {
        onView(withText(R.string.create_identity_create)).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.create_identity_enter_name)).check(matches(isDisplayed()));
        UITestHelper.screenShot(UITestHelper.getCurrentActivity(mActivity), "input");
        onView(withId(R.id.et_name)).perform(setText(name));
        onView(withText(R.string.next)).perform(click());
    }

    private void createIdentityEnterEmail() throws Throwable {
        onView(withText(R.string.create_identity_enter_email)).check(matches(isDisplayed()));
        UITestHelper.screenShot(UITestHelper.getCurrentActivity(mActivity), "input");
        onView(withId(R.id.et_name)).perform(setText(mail), pressImeActionButton());
    }

    private void createIdentityEnterPhone() throws Throwable {
        onView(withText(R.string.create_identity_enter_phone)).check(matches(isDisplayed()));
        UITestHelper.screenShot(UITestHelper.getCurrentActivity(mActivity), "input");
        onView(withId(R.id.et_name)).perform(setText(phone), pressImeActionButton());
    }

    private void createIdentityPerformConfirm() {
        onView(withText(R.string.create_identity_successful)).check(matches(isDisplayed()));
        onView(withText(R.string.create_identity_final_text)).check(matches(isDisplayed()));
        onView(withText(name)).check(matches(isDisplayed()));
        onView(withText(mail)).check(matches(isDisplayed()));
        onView(withText(phoneFormatted)).check(matches(isDisplayed()));
        onView(withText(R.string.finish)).perform(click());
    }
}

