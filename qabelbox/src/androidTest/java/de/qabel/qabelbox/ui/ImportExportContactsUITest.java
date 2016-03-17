package de.qabel.qabelbox.ui;

import android.os.PowerManager;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import com.squareup.spoon.Spoon;

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
import de.qabel.qabelbox.ui.helper.DocumentIntender;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
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
	private String TAG = this.getClass().getSimpleName();

	DocumentIntender intender = new DocumentIntender();
	private Identity identity;

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
//		mBoxHelper.deleteAllContacts(identity);
		mBoxHelper.deleteAllIdentities();
		identity=mBoxHelper.addIdentity("spoon123");


		user1 = mBoxHelper.addIdentity("user1");
		user2 = mBoxHelper.addIdentity("user2");
		user3 = mBoxHelper.addIdentity("user3");

		mBoxHelper.setActiveIdentity(identity);

		contact1Json = ContactExportImport.exportIdentityAsContact(user1);
		contact2Json = ContactExportImport.exportIdentityAsContact(user2);
		contact3Json = ContactExportImport.exportIdentityAsContact(user3);

		assertThat(mBoxHelper.getService().getContacts().getContacts().size() ,is(0));
		addContact(identity, contact1Json);
		addContact(identity, contact2Json);
		addContact(identity,contact3Json);
		assertThat(mBoxHelper.getService().getContacts().getContacts().size() ,is(3));


	}

	private void addContact(Identity identity,String contactJSON) {
		try {
			mBoxHelper.getService().addContact(new ContactExportImport().parseContactForIdentity(identity, new JSONObject(contactJSON)));
		} catch (Exception e) {
			assertNotNull(e);
			Log.e(TAG, "error on add contact", e);
		}
	}


	@Test
	public void testExportContact() {
		Spoon.screenshot(mActivity, "exportOne");
		File file1 = new File(mActivity.getCacheDir(), "testexportcontact");

		onView(withId(R.id.drawer_layout)).check(matches(isDisplayed())).perform(QabelViewAction.actionOpenDrawer());
		UITestHelper.sleep(1000);

		onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
				.perform(click());
		Spoon.screenshot(mActivity, "contacts");

		//onView(withText("user1")).check(matches(isDisplayed())).perform(click());
		intender.handleAddFileIntent(file1);
		onView(withId(R.id.contact_list))
				.perform(RecyclerViewActions.actionOnItem(
						hasDescendant(withText("user1")), longClick()));
	}

	@Test
	public void testExportContacts() {
		Spoon.screenshot(mActivity, "exportAll");
	}

	@Test
	public void testImportContact() {
		Spoon.screenshot(mActivity, "importOne");
	}

	@Test
	public void testImportContacts() {
		Spoon.screenshot(mActivity, "importAll");
	}

}
