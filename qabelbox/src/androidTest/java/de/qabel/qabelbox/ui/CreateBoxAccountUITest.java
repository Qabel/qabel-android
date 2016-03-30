package de.qabel.qabelbox.ui;


import android.os.PowerManager.WakeLock;
import android.support.test.rule.ActivityTestRule;
import android.text.InputType;
import com.squareup.spoon.Spoon;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.TestConstraints;
import de.qabel.qabelbox.activities.CreateAccountActivity;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateBoxAccountUITest extends UIBoxHelper {
    @Rule
    public ActivityTestRule<CreateAccountActivity> mActivityTestRule = new ActivityTestRule<>(CreateAccountActivity.class, false, true);

    private CreateAccountActivity mActivity;

    private WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;


    @After
    public void cleanUp() {
        wakeLock.release();
        mSystemAnimations.enableAll();
        unbindService(QabelBoxApplication.getInstance());
    }


    @Before
    public void setUp() throws IOException, QblStorageException {
        mActivity = mActivityTestRule.getActivity();
        URLs.setBaseAccountingURL(TestConstants.ACCOUNTING_URL);

        bindService(QabelBoxApplication.getInstance());
        createTokenIfNeeded(false);


        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }


    private void clearIdentities() {
        removeAllIdentities();
        new AppPreference(QabelBoxApplication.getInstance()).setToken(null);
    }


    @Test
    public void createBoxAccountTest() throws Throwable {
        clearIdentities();
        pressBack();
        onView(withText(String.format(mActivity.getString(string.message_step_is_needed_or_close_app), string.boxaccount)));
        onView(withText(string.no)).perform(click());

        onView(withText(string.create_box_account)).perform(click());
        String accountName = UUID.randomUUID().toString().substring(0, 15).replace("-", "x");
        String duplicateName = "example";//example exists on server
        String failEMail = "example@example.";//incorrect email
        String duplicateEMail = "example@example.com";//email exists on server
        String failPassword = "12345678";
        String password = "passwort12$";

        createExistingUser(duplicateName, duplicateEMail, password);
        //enter name
        testNameExists(duplicateName);
        enterSingleLine(accountName, "name", false);

        //enter email
        testEMailExists(duplicateEMail);
        testFailEmail(failEMail);
        enterSingleLine(accountName + "@example.com", "email", true);

        //check password
        checkNumericPassword(failPassword);
        checkBadPassword(accountName, failPassword);
        checkPassword(password);

        checkSuccess();
    }

    private void createExistingUser(String duplicateName, String duplicateEMail, String password) {
        final CountDownLatch cl = new CountDownLatch(1);
        new BoxAccountRegisterServer().register(duplicateName, password, password, duplicateEMail, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                cl.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                cl.countDown();
            }
        });
        try {
            cl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    private void checkSuccess() throws Throwable {
        onView(withText(string.create_account_final_headline)).check(matches(isDisplayed()));
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "result");
        onView(withText(string.btn_create_identity)).check(matches(isDisplayed())).perform(click());

        onView(withText(string.headline_add_identity)).check(matches(isDisplayed()));
        assertNotNull(new AppPreference(QabelBoxApplication.getInstance()).getToken());
    }

    private void checkPassword(String password) throws Throwable {
        //Check Passwords dont match
        onView(withId(id.et_password1)).perform(typeText(password), pressImeActionButton());
        closeKeyboard();
        onView(withText(string.next)).perform(click());

        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "passwordNotMatch");
        onView(withText(string.create_account_passwords_dont_match)).check(matches(isDisplayed()));
        onView(withText(string.ok)).perform(click());

        //enter password 2 and press next
        onView(withId(id.et_password2)).check(matches(isDisplayed())).perform(click());
        onView(withId(id.et_password2)).perform(typeText(password), pressImeActionButton());
        closeKeyboard();
        onView(withText(string.next)).perform(click());
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "password2");

        waitForServer();
    }

    private void waitForServer() {
        UITestHelper.sleep(TestConstraints.SIMPLE_SERVER_ACTION_TIMEOUT);
    }

    private void checkBadPassword(String accountName, String failPassword) throws Throwable {
        //Check accountname in password validation
        onView(withId(id.et_password1)).perform(typeText(failPassword + accountName), pressImeActionButton());
        onView(withId(id.et_password2)).perform(typeText(failPassword + accountName), pressImeActionButton());
        closeKeyboard();
        onView(withText(string.next)).perform(click());

        onView(withText(string.password_contains_user)).check(matches(isDisplayed()));
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "accountNamePasswords");
        onView(withText(string.ok)).perform(click());

        onView(withId(id.et_password1)).perform(clearText());
        onView(withId(id.et_password2)).perform(clearText());
    }

    private void closeKeyboard() {
        closeSoftKeyboard();
        UITestHelper.sleep(500);
    }

    private void checkNumericPassword(String failPassword) throws Throwable {
        //Check numeric validation
        onView(withId(id.et_password1)).perform(typeText(failPassword), pressImeActionButton());
        onView(withId(id.et_password2)).perform(typeText(failPassword), pressImeActionButton());
        closeKeyboard();
        onView(withText(string.next)).perform(click());

        onView(withText(string.password_digits_only)).check(matches(isDisplayed()));
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "numericPasswords");
        onView(withText(string.ok)).perform(click());

        onView(withId(id.et_password1)).perform(clearText());
        onView(withId(id.et_password2)).perform(clearText());
    }

    private void testFailEmail(String failEMail) throws Throwable {
        enterSingleLine(failEMail, "failEmail", true);
        waitForServer();
        onView(withText(string.dialog_headline_info)).check(matches(isDisplayed()));
        onView(withText(string.ok)).check(matches(isDisplayed())).perform(click());
        onView(withId(id.et_name)).perform(clearText());
    }

    private void testEMailExists(String failEMail) throws Throwable {
        enterSingleLine(failEMail, "emailExists", true);
        waitForServer();
        onView(withText(string.dialog_headline_info)).check(matches(isDisplayed()));
        onView(withText(string.ok)).check(matches(isDisplayed())).perform(click());
        onView(withId(id.et_name)).perform(clearText());
    }

    private void testNameExists(String failName) throws Throwable {
        enterSingleLine(failName, "nameExists", false);
        waitForServer();
        onView(withText(string.dialog_headline_info)).check(matches(isDisplayed()));
        onView(withText(string.ok)).check(matches(isDisplayed())).perform(click());
        onView(withId(id.et_name)).perform(clearText());
    }


    private void enterSingleLine(String accountName, String screenName, boolean checkFieldsIsEmail) throws Throwable {
        onView(withId(id.et_name)).check(matches(isDisplayed())).perform(click());
        if (checkFieldsIsEmail) {
            onView(withId(id.et_name)).check(matches(withInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)));
        } else {
            onView(withId(id.et_name)).check(matches(withInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)));
        }
        onView(allOf(withClassName(endsWith("EditTextFont")))).perform(typeText(accountName), pressImeActionButton());
        closeSoftKeyboard();
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), screenName);

        onView(withText(string.next)).perform(click());
        UITestHelper.sleep(500);
    }


}

