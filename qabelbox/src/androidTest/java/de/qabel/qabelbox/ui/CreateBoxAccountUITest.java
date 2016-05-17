package de.qabel.qabelbox.ui;

/**
 * Created by danny on 05.01.2016.
 */

import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.text.InputType;

import com.squareup.spoon.Spoon;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.TestConstraints;
import de.qabel.qabelbox.activities.CreateAccountActivity;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.communication.callbacks.JsonRequestCallback;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.idling.InjectedIdlingResource;
import okhttp3.Response;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withInputType;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static de.qabel.qabelbox.ui.action.QabelViewAction.setText;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

public class CreateBoxAccountUITest extends UIBoxHelper {

    @Rule
    public ActivityTestRule<CreateAccountActivity> mActivityTestRule = new ActivityTestRule<>(CreateAccountActivity.class, false, false);

    private CreateAccountActivity mActivity;

    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;
    private InjectedIdlingResource idlingResource;


    @After
    public void cleanUp() {

        if (wakeLock != null) {
            wakeLock.release();
        }
        if (mSystemAnimations != null) {
            mSystemAnimations.enableAll();
        }
        unbindService(QabelBoxApplication.getInstance());
        if (idlingResource !=null) {
            Espresso.unregisterIdlingResources(idlingResource);
        }
    }


    @Before
    public void setUp() throws IOException, QblStorageException {

        URLs.setBaseAccountingURL(TestConstants.ACCOUNTING_URL);

        bindService(QabelBoxApplication.getInstance());
        new AppPreference(InstrumentationRegistry.getTargetContext()).clear();
        removeAllIdentities();

        mActivity = mActivityTestRule.launchActivity(null);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
        idlingResource = new InjectedIdlingResource();
        mActivity.injectIdleCallback(idlingResource);
        Espresso.registerIdlingResources(idlingResource);
    }


    @Test
    public void testLoginToBoxAccount() throws Throwable {
        String accountName = generateUsername();
        String accountEMail = accountName + "@example.com";
        String password = "passwort12$";
        String incorrectPassword = "incorrectPassword";
        createBoxAccountWithoutUI(accountName, accountEMail, password);
        onView(withText(R.string.login)).perform(click());

        //enter incorrect credentials
        onView(withId(R.id.et_username)).perform(setText(accountName), pressImeActionButton());
        onView(withId(R.id.et_password)).perform(setText(incorrectPassword), pressImeActionButton());
        closeKeyboard();

        onView(withText(R.string.next)).perform(click());
        UITestHelper.waitForView(R.string.ok, TestConstraints.SIMPLE_SERVER_ACTION_TIMEOUT);
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "incorrectCredentials");
        onView(withText(R.string.ok)).perform(click());

        createBoxAccountWithoutUI(accountName, accountEMail, password);

        //enter correct credentials
        onView(withId(R.id.et_password)).perform(setText(password), pressImeActionButton());
        closeKeyboard();
        AppPreference appPrefs = new AppPreference(QabelBoxApplication.getInstance());
        assertNull(appPrefs.getToken());

        //check result
        onView(withText(R.string.next)).perform(click());
        UITestHelper.sleep(TestConstraints.SIMPLE_SERVER_ACTION_TIMEOUT);
        assertNotNull(appPrefs.getToken());
        assertThat(appPrefs.getAccountName(), is(accountName));
    }

    @Test
    public void testCreateBoxAccountTest() throws Throwable {
        onView(withText(R.string.create_box_account)).perform(click());
        String accountName = generateUsername();
        String accountEMail = accountName + "@example.com";
        String password = "passwort12$";
        enterSingleLine(accountName, "name", false);
        enterSingleLine(accountEMail, "email", true);
        onView(withId(R.id.et_password1)).perform(setText(password), pressImeActionButton());
        onView(withId(R.id.et_password2)).perform(setText(password), pressImeActionButton());
        closeKeyboard();
        onView(withText(R.string.next)).perform(click());
        UITestHelper.waitForView(R.string.create_account_final_headline, TestConstraints.SIMPLE_SERVER_ACTION_TIMEOUT);
        onView(withText(R.string.create_account_final_headline)).check(matches(isDisplayed()));
        checkSuccess(accountName, accountEMail);
    }

    @Test
    public void testCreateBoxAccountErrorsTest() throws Throwable {
        pressBack();
        onView(withText(String.format(mActivity.getString(R.string.message_step_is_needed_or_close_app), R.string.boxaccount)));
        onView(withText(R.string.no)).perform(click());

        onView(withText(R.string.create_box_account)).perform(click());
        String accountName = generateUsername();
        String accountEMail = accountName + "@example.com";

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
        enterSingleLine(accountEMail, "email", true);

        //check password
        checkNumericPassword(failPassword);
        checkBadPassword(accountName, failPassword);
        checkPassword(password);
    }

    private String generateUsername() {
        return UUID.randomUUID().toString().substring(0, 15).replace("-", "x");
    }

    private void createBoxAccountWithoutUI(String accountName, String accountEMail, String password) {
        final CountDownLatch cl = new CountDownLatch(1);
        new BoxAccountRegisterServer(mActivity.getApplicationContext()).register(accountName, password, password, accountEMail, new JsonRequestCallback(new int[]{200, 201, 400}) {

            @Override
            protected void onError(Exception e, @Nullable Response response) {
                fail("can't create user for login");
            }

            @Override
            protected void onJSONSuccess(Response response, JSONObject result) {
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


    private void createExistingUser(String duplicateName, String duplicateEMail, String password) {
        final CountDownLatch cl = new CountDownLatch(1);
        new BoxAccountRegisterServer(mActivity.getApplicationContext()).register(duplicateName, password, password, duplicateEMail, new JsonRequestCallback(new int[]{200, 400}) {
            @Override
            protected void onError(Exception e, @Nullable Response response) {
                cl.countDown();
                fail("Could not check create existing user");
            }

            @Override
            protected void onJSONSuccess(Response response, JSONObject result) {
                cl.countDown();
                assertNotNull(BoxAccountRegisterServer.parseJson(result).username);
            }
        });
        try {
            cl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    private void checkSuccess(String accountName, String accountEmail) throws Throwable {
        onView(withText(R.string.create_account_final_headline)).check(matches(isDisplayed()));
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "result");
        onView(withText(R.string.btn_create_identity)).check(matches(isDisplayed())).perform(click());

        onView(withText(R.string.headline_add_identity)).check(matches(isDisplayed()));
        AppPreference appPrefs = new AppPreference(QabelBoxApplication.getInstance());
        assertNotNull(appPrefs.getToken());
        assertThat(appPrefs.getAccountName(), is(accountName));
        assertThat(appPrefs.getAccountEMail(), is(accountEmail));
    }

    private void checkPassword(String password) throws Throwable {
        //Check Passwords dont match
        onView(withId(R.id.et_password1)).perform(setText(password), pressImeActionButton());
        closeKeyboard();
        onView(withText(R.string.next)).perform(click());
        onView(withText(R.string.create_account_passwords_dont_match)).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).perform(click());

        //enter password 2 and press next
        onView(withId(R.id.et_password2)).perform(setText(password), pressImeActionButton());
        closeKeyboard();
        onView(withText(R.string.next)).perform(click());
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "password2");


    }


    private void checkBadPassword(String accountName, String failPassword) throws Throwable {
        //Check accountname in password validation
        onView(withId(R.id.et_password1)).perform(setText(failPassword + accountName), pressImeActionButton());
        onView(withId(R.id.et_password2)).perform(setText(failPassword + accountName), pressImeActionButton());
        closeKeyboard();
        onView(withText(R.string.next)).perform(click());

        onView(withText(R.string.password_contains_user)).check(matches(isDisplayed()));
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "accountNamePasswords");
        onView(withText(R.string.ok)).perform(click());

        onView(withId(R.id.et_password1)).perform(clearText());
        onView(withId(R.id.et_password2)).perform(clearText());
    }

    private void closeKeyboard() {
        closeSoftKeyboard();
        UITestHelper.sleep(500);
    }

    private void checkNumericPassword(String failPassword) throws Throwable {
        //Check numeric validation
        onView(withId(R.id.et_password1)).perform(setText(failPassword), pressImeActionButton());
        onView(withId(R.id.et_password2)).perform(setText(failPassword), pressImeActionButton());
        closeKeyboard();
        onView(withText(R.string.next)).perform(click());

        onView(withText(R.string.password_digits_only)).check(matches(isDisplayed()));
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "numericPasswords");
        onView(withText(R.string.ok)).perform(click());

        onView(withId(R.id.et_password1)).perform(clearText());
        onView(withId(R.id.et_password2)).perform(clearText());
    }

    private void testFailEmail(String failEMail) throws Throwable {
        enterSingleLine(failEMail, "failEmail", true);
        UITestHelper.waitForView(R.string.ok, TestConstraints.SIMPLE_SERVER_ACTION_TIMEOUT);
        onView(withText(R.string.dialog_headline_info)).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.et_name)).perform(clearText());
    }

    private void testEMailExists(String failEMail) throws Throwable {
        enterSingleLine(failEMail, "emailExists", true);
        UITestHelper.waitForView(R.string.ok, TestConstraints.SIMPLE_SERVER_ACTION_TIMEOUT);
        onView(withText(R.string.dialog_headline_info)).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.et_name)).perform(clearText());
    }

    private void testNameExists(String failName) throws Throwable {
        enterSingleLine(failName, "nameExists", false);
        UITestHelper.waitForView(R.string.ok, TestConstraints.SIMPLE_SERVER_ACTION_TIMEOUT);
        onView(withText(R.string.dialog_headline_info)).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.et_name)).perform(clearText());
    }


    private void enterSingleLine(String accountName, String screenName, boolean checkFieldsIsEmail) throws Throwable {
        onView(withId(R.id.et_name)).check(matches(isDisplayed())).perform(click());
        if (checkFieldsIsEmail) {
            onView(withId(R.id.et_name)).check(matches(withInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)));
        } else {
            onView(withId(R.id.et_name)).check(matches(withInputType(InputType.TYPE_CLASS_TEXT)));
        }
        onView(allOf(withClassName(endsWith("EditTextFont")))).perform(setText(accountName), pressImeActionButton());
        closeSoftKeyboard();
        Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), screenName);

        onView(withText(R.string.next)).perform(click());
    }
}

