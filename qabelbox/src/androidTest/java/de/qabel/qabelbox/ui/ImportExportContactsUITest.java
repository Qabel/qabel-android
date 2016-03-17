package de.qabel.qabelbox.ui;

import android.os.PowerManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import com.squareup.spoon.Spoon;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.ui.helper.DocumentIntents;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
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
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
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
	private final String TAG = this.getClass().getSimpleName();

	private final DocumentIntents intending = new DocumentIntents();
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
		mBoxHelper.deleteAllIdentities();
		identity = mBoxHelper.addIdentity("spoon123");


		createTestContacts();
	}

	private void createTestContacts() {
		Identity user1 = mBoxHelper.addIdentity("user1");
		Identity user2 = mBoxHelper.addIdentity("user2");
		Identity user3 = mBoxHelper.addIdentity("user3");

		mBoxHelper.setActiveIdentity(identity);

		String contact1Json = ContactExportImport.exportIdentityAsContact(user1);
		String contact2Json = ContactExportImport.exportIdentityAsContact(user2);
		String contact3Json = ContactExportImport.exportIdentityAsContact(user3);

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

	private void checkMessageBox() {
		onView(withText(R.string.dialog_headline_info)).check(matches(isDisplayed()));
		onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
	}

	private void goToContacts() {
		DrawerActions.openDrawer(R.id.drawer_layout);

		onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
				.perform(click());
		Spoon.screenshot(mActivity, "contacts");
	}

	private JSONObject checkFile(File file1) {
		try {
			FileInputStream fis = new FileInputStream(file1);
			assertTrue(file1.exists());
			return new JSONObject(FileHelper.readFileAsText(fis));
		} catch (IOException | JSONException e) {
			e.printStackTrace();
			assertNull(e);
			return null;
		}
	}
	@Test
	public void testExportSingleContact() {

		String userName = "user1";
		File file1 = new File(mActivity.getCacheDir(), "testexportcontact");
		assertNotNull(file1);
		goToContacts();

		onView(withId(R.id.contact_list))
				.perform(RecyclerViewActions.actionOnItem(
						hasDescendant(withText(userName)), longClick()));
		Spoon.screenshot(mActivity, "exportOne");
		Intents.init();
		intending.handleSaveFileIntent(file1);
		onView(withText(R.string.Export)).check(matches(isDisplayed())).perform(click());
		Intents.release();
		checkMessageBox();

		try {
			Contact contact = ContactExportImport.parseContactForIdentity(identity, checkFile(file1));
			assertEquals(contact.getAlias(), userName);
		} catch (JSONException e) {
			e.printStackTrace();
			assertNull(e);
		}

	}

	@Test
	public void testExportManyContact() {

		File file1 = new File(mActivity.getCacheDir(), "testexportallcontact");
		assertNotNull(file1);
		goToContacts();
		openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

		Spoon.screenshot(mActivity, "exportAll");
		Intents.init();
		intending.handleSaveFileIntent(file1);
		onView(withText(R.string.contact_export_all)).perform(click());
		Intents.release();
		checkMessageBox();

		try {
			Contacts contact = ContactExportImport.parseContactsForIdentity(identity, checkFile(file1));
			assertEquals(contact.getContacts().size(), 3);
		} catch (JSONException e) {
			e.printStackTrace();
			assertNull(e);
		}

	}

	@Test
	public void testImportSingleContact() {
		String userToImport = "importUser1";
		Identity importUser1 = mBoxHelper.addIdentity(userToImport);
		String exportUser = ContactExportImport.exportIdentityAsContact(importUser1);
		File file1 = new File(mActivity.getCacheDir(), "testexportallcontact");
		saveJsonIntoFile(exportUser, file1);

		assertNotNull(file1);
		goToContacts();
		onView(withId(R.id.fab)).perform(click());

		Spoon.screenshot(mActivity, "exportAll");
		Intents.init();
		intending.handleLoadFileIntent(file1);
		onView(withText(R.string.from_file)).perform(click());
		Intents.release();
		checkMessageBox();
		onView(withText(userToImport)).check(matches(isDisplayed()));

	}

	private void saveJsonIntoFile(String exportUser, File file1) {
		try {
			FileOutputStream fos = new FileOutputStream(file1);
			fos.write(exportUser.getBytes());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




}
