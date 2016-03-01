package de.qabel.qabelbox.ui;

/**
 * Created by danny on 05.01.2016.
 */

import android.os.PowerManager;
import android.support.test.rule.ActivityTestRule;

import com.squareup.spoon.Spoon;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.UUID;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.CreateAccountActivity;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateBoxAccountTest {

	@Rule
	public ActivityTestRule<CreateAccountActivity> mActivityTestRule = new ActivityTestRule<>(CreateAccountActivity.class, false, true);

	private CreateAccountActivity mActivity;

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
		new AppPreference(QabelBoxApplication.getInstance()).setToken(null);
	}


	@Test
	public void createBoxAccountTest() throws Throwable {
		clearIdentities();
		onView(withText(R.string.create_box_account)).perform(click());
		String accountName = UUID.randomUUID().toString().substring(0, 15).replace("-", "x");
		String password = "passwort12$";
		//enter name
		enterSingleLine(accountName, "name");
		enterSingleLine(accountName + "@qabel.de", "email");

		//enter password 1 and press next
		onView(withId(R.id.et_password1)).check(matches(isDisplayed())).perform(click());
		onView(withId(R.id.et_password1)).perform(typeText(password), pressImeActionButton());
		closeSoftKeyboard();
		//UITestHelper.sleep(500);
		onView(withText(R.string.next)).perform(click());
		Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "passwordNotMatch");
		onView(withText(R.string.ok)).perform(click());
		//onView(allOf(withClassName(endsWith("nFont")))).perform(click());

		//enter password 2 and press next
		onView(withId(R.id.et_password2)).check(matches(isDisplayed())).perform(click());
		onView(withId(R.id.et_password2)).perform(typeText(password), pressImeActionButton());
		closeSoftKeyboard();
		UITestHelper.sleep(500);
		onView(withText(R.string.next)).perform(click());
		Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "password2");

		onView(withText(R.string.create_account_final_headline)).check(matches(isDisplayed()));
		Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "result");
		onView(withText(R.string.btn_create_identity)).check(matches(isDisplayed())).perform(click());

		onView(withText(R.string.headline_add_identity)).check(matches(isDisplayed()));
		assertNotNull(new AppPreference(QabelBoxApplication.getInstance()).getToken());

	}

	protected void enterSingleLine(String accountName, String screenName) throws Throwable {
		onView(withId(R.id.et_name)).check(matches(isDisplayed())).perform(click());
		onView(allOf(withClassName(endsWith("EditTextFont")))).perform(typeText(accountName), pressImeActionButton());
		closeSoftKeyboard();
		Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), screenName);
		UITestHelper.sleep(500);
		onView(withText(R.string.next)).perform(click());
	}


}

