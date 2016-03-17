package de.qabel.qabelbox.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import com.squareup.spoon.Spoon;

import org.hamcrest.core.AllOf;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.action.QabelViewAction;
import de.qabel.qabelbox.ui.helper.DocumentIntents;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

/**
 * Created by danny on 17.003.2016.
 */
public class ImportExportContactsUITest {
	@Rule
	public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, true);
	private MainActivity mActivity;
	private UIBoxHelper mBoxHelper;
	private PowerManager.WakeLock wakeLock;
	private SystemAnimations mSystemAnimations;
	private Identity user1, user2, user3;
	private String contact1Json, contact2Json, contact3Json;
	private final String TAG = this.getClass().getSimpleName();

	private final DocumentIntents intending = new DocumentIntents();

	public ImportExportContactsUITest() throws IOException {
		setupData();
	}

	@After
	public void cleanUp() {

		wakeLock.release();
		mSystemAnimations.enableAll();
		mBoxHelper.unbindService(QabelBoxApplication.getInstance());
	}

	@Before
	public void setUp() throws IOException, QblStorageException {

		mActivity = mActivityTestRule.getActivity();

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
		mBoxHelper.deleteAllIdentities();
		Identity identity = mBoxHelper.addIdentity("spoon123");


		user1 = mBoxHelper.addIdentity("user1");
		user2 = mBoxHelper.addIdentity("user2");
		user3 = mBoxHelper.addIdentity("user3");

		mBoxHelper.setActiveIdentity(identity);

		contact1Json = ContactExportImport.exportIdentityAsContact(user1);
		contact2Json = ContactExportImport.exportIdentityAsContact(user2);
		contact3Json = ContactExportImport.exportIdentityAsContact(user3);

		assertThat(mBoxHelper.getService().getContacts().getContacts().size(), is(0));
		addContact(identity, contact1Json);
		addContact(identity, contact2Json);
		addContact(identity, contact3Json);
		assertThat(mBoxHelper.getService().getContacts().getContacts().size(), is(3));
	}

	private void addContact(Identity identity, String contactJSON) {
		try {
			mBoxHelper.getService().addContact(new ContactExportImport().parseContactForIdentity(identity, new JSONObject(contactJSON)));
		} catch (Exception e) {
			assertNotNull(e);
			Log.e(TAG, "error on add contact", e);
		}
	}


	@Test
	public void testExportContact() {


		File file1 = new File(mActivity.getCacheDir(), "testexportcontact");
		assertNotNull(file1);


		onView(withId(R.id.drawer_layout)).check(matches(isDisplayed())).perform(QabelViewAction.actionOpenDrawer());
		UITestHelper.sleep(1000);

		onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
				.perform(click());
		Spoon.screenshot(mActivity, "contacts");

		onView(withId(R.id.contact_list))
				.perform(RecyclerViewActions.actionOnItem(
						hasDescendant(withText("user1")), longClick()));
		Spoon.screenshot(mActivity, "exportOne");
		Intents.init();
	//	intending.handleSaveFileIntent(file1);
		Intent data = new Intent();
		data.setData(Uri.fromFile(file1));

		Intents.intending(AllOf.allOf(
				hasAction(Intent.ACTION_CREATE_DOCUMENT),
				hasCategories(hasItem(Intent.CATEGORY_OPENABLE))
		)).respondWith(
				new Instrumentation.ActivityResult(Activity.RESULT_OK, data)
		);
		onView(withText(R.string.Export)).check(matches(isDisplayed())).perform(click());
		Intents.release();
		onView(withText(R.string.contact_export_successfully)).check(matches(isDisplayed()));

	}
/*
	@Test
	public void testExportContacts() {

	}

	@Test
	public void testImportContact() {

	}

	@Test
	public void testImportContacts() {

	}*/

}
