package de.qabel.qabelbox.ui;

/**
 * Created by danny on 05.01.2016.
 */

import android.graphics.Bitmap;
import android.os.PowerManager;
import android.support.design.internal.NavigationMenuItemView;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;

import com.squareup.picasso.PicassoIdlingResource;
import com.squareup.spoon.Spoon;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.exceptions.QblStorageException;
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
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.test.MoreAsserts.assertNotEmpty;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
//import static de.qabel.qabelbox.ui.matcher.QabelMatcher.withDrawable;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ChatMessageUITest {

	@Rule
	public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, true);
	private MainActivity mActivity;
	private UIBoxHelper mBoxHelper;
	private final boolean mFillAccount = true;
	private PowerManager.WakeLock wakeLock;
	private final PicassoIdlingResource mPicassoIdlingResource = new PicassoIdlingResource();
	private SystemAnimations mSystemAnimations;
	private Identity user1, user2;
	private String contact1Json, contact2Json;
	private String TAG = this.getClass().getSimpleName();

	public ChatMessageUITest() throws IOException {
		//setup data before MainActivity launched. This avoid the call to create identity
		if (mFillAccount) {
			setupData();
		}
	}

	@After
	public void cleanUp() {

		wakeLock.release();
		Espresso.unregisterIdlingResources(mPicassoIdlingResource);
		mSystemAnimations.enableAll();
		mBoxHelper.unbindService(QabelBoxApplication.getInstance());
	}

	@Before
	public void setUp() throws IOException, QblStorageException {

		mActivity = mActivityTestRule.getActivity();
		Espresso.registerIdlingResources(mPicassoIdlingResource);
		ActivityLifecycleMonitorRegistry
				.getInstance()
				.addLifecycleCallback(mPicassoIdlingResource);
		wakeLock = UIActionHelper.wakeupDevice(mActivity);
		mSystemAnimations = new SystemAnimations(mActivity);
		mSystemAnimations.disableAll();
	}

	private void setupData() {
		URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
		mActivity = mActivityTestRule.getActivity();
		mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
		mBoxHelper.bindService(QabelBoxApplication.getInstance());
		mBoxHelper.createTokenIfNeeded(false);
		try {
			Identity old = mBoxHelper.getCurrentIdentity();
			if (old != null) {
				mBoxHelper.deleteIdentity(old);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		mBoxHelper.removeAllIdentities();
		user1 = mBoxHelper.addIdentity("user1");
		user2 = mBoxHelper.addIdentity("user2");
		contact1Json = ContactExportImport.exportIdentityAsContact(user1);
		contact2Json = ContactExportImport.exportIdentityAsContact(user2);
		mBoxHelper.setActiveIdentity(user1);
		try {
			mBoxHelper.getService().addContact(new ContactExportImport().parseContactForIdentity(user1, new JSONObject(contact2Json)));
		} catch (Exception e) {
			Log.e(TAG, "error on add contact", e);
		}
		assertNotEmpty(mBoxHelper.getService().getContacts().getContacts());
		mBoxHelper.setActiveIdentity(user2);
		try {
			mBoxHelper.getService().addContact(new ContactExportImport().parseContactForIdentity(user2, new JSONObject(contact1Json)));
		} catch (Exception e) {
			Log.e(TAG, "error on add contact", e);
		}
		assertNotEmpty(mBoxHelper.getService().getContacts().getContacts());
		uploadTestFiles();
	}

	private void uploadTestFiles() {

		int fileCount = 1;
		mBoxHelper.uploadDrawableFile(mBoxHelper.mBoxVolume, "file3.png", Bitmap.CompressFormat.PNG, R.drawable.qabel_logo);
		mBoxHelper.waitUntilFileCount(fileCount);
	}

	@Test
	public void testSendMessage() {
		mPicassoIdlingResource.init(mActivity);
		Spoon.screenshot(mActivity, "empty");
		sendOneAndCheck(1);
		sendOneAndCheck(2);
	}

	protected void sendOneAndCheck(int messages) {
		openDrawer(R.id.drawer_layout);

		onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
				.perform(click());
		Spoon.screenshot(mActivity, "contacts");

		//ContactList and click on user
		onView(withId(R.id.contact_list)).check(matches(isDisplayed()));
		onView(withText("user1")).perform(click());

		//ChatView is displayed
		onView(withId(R.id.contact_chat_list)).check(matches(isDisplayed()));

		//Check Username is displayed in chatview
		QabelMatcher.matchToolbarTitle("user1").check(matches(isDisplayed()));

		onView(withId(R.id.etText)).check(matches(isDisplayed())).perform(click());
		onView(withId(R.id.etText)).perform(typeText("text" + messages), pressImeActionButton());
		closeSoftKeyboard();
		onView(withText(R.string.btn_chat_send)).check(matches(isDisplayed())).perform(click());
		UITestHelper.sleep(1000);

		onView(withId(R.id.contact_chat_list)).
				check(matches(isDisplayed())).
				check(matches(QabelMatcher.withListSize(messages)));
		pressBack();

		//go to identity user 1
		openDrawer(R.id.drawer_layout);
		onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
		UITestHelper.sleep(500);
		onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText("user1"))).perform(click());

		openDrawer(R.id.drawer_layout);
		onView(withText(R.string.Contacts)).check(matches(isDisplayed())).perform(click());
		Spoon.screenshot(mActivity, "message" + messages);

		onView(withText("user2")).check(matches(isDisplayed())).perform(click());
		onView(withId(R.id.contact_chat_list)).
				check(matches(isDisplayed())).
				check(matches(QabelMatcher.withListSize(messages)));
		pressBack();

		//go to user 2
		openDrawer(R.id.drawer_layout);
		onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
		UITestHelper.sleep(500);
		onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText("user2"))).perform(click());
		openDrawer(R.id.drawer_layout);
	}

}

