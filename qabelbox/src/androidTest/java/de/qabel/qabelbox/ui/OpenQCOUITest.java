package de.qabel.qabelbox.ui;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;

import com.squareup.spoon.Spoon;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.config.IdentityExportImport;
import de.qabel.qabelbox.config.QabelSchema;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

/**
 * Created by danny on 17.003.2016.
 */
public class OpenQCOUITest {
	@Rule
	public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, false);
	private MainActivity mActivity;
	private UIBoxHelper mBoxHelper;
	private final String TAG = this.getClass().getSimpleName();

	public OpenQCOUITest() throws IOException {
		setupData();
	}

	@After
	public void cleanUp() {

		mBoxHelper.unbindService(QabelBoxApplication.getInstance());
	}

	@Before
	public void setUp() throws IOException, QblStorageException {

	}

	@Test
	public void testOpenQcoFileFromExternal() {
		String userToImport = "contact1";
		File tempQcoFile = createQcoFile(userToImport, QabelSchema.FILE_SUFFIX_CONTACT);

		launchExternalIntent(Uri.fromFile(tempQcoFile));
		checkMessageBox();
		tempQcoFile.delete();
		goToContacts();
		Spoon.screenshot(mActivity, "openQCO");
		onView(withText(userToImport)).check(matches(isDisplayed()));

	}

	/*@Test
	public void testOpenQcoSanityFromExternal() {
		String userToImport = "contact1";
		File tempQcoFile = createQcoFile(userToImport, QabelSchema.FILE_SUFFIX_CONTACT);

		launchExternalIntent(Uri.fromFile(tempQcoFile));
		checkMessageBox();
		tempQcoFile.delete();
		goToContacts();
		Spoon.screenshot(mActivity, "openQCO");
		onView(withText(userToImport)).check(matches(isDisplayed()));

	}*/

	@Test
	public void testOpenUnknownFileTypeFromExternal() {
		String userToImport = "contact1";
		File tempQcoFile = createQcoFile(userToImport, ".unknown");
		launchExternalIntent(Uri.fromFile(tempQcoFile));
		onView(withText(R.string.cant_import_file_type_is_unknown)).check(matches(isDisplayed()));
		Spoon.screenshot(mActivity, "openQCO");
		onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
		tempQcoFile.delete();
	}

	@Test
	public void testOpenCorruptContactFromExternal() {
		String userToImport = "defectcontaact";
		File tempQcoFile = createQcoFile(userToImport,  QabelSchema.FILE_SUFFIX_CONTACT);
		try {
			FileOutputStream fis = new FileOutputStream(tempQcoFile);
			fis.write(1);
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		launchExternalIntent(Uri.fromFile(tempQcoFile));
		Spoon.screenshot(mActivity, "corruptContact");
		onView(withText(R.string.contact_import_failed)).check(matches(isDisplayed()));
		onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
		tempQcoFile.delete();
	}

	@Test
	public void testOpenQidFileFromExternal() {
		String userToImport = "identity1.qid";
		File tempQidFile = createQIDFile(userToImport);

		launchExternalIntent(Uri.fromFile(tempQidFile));
		onView(withText(R.string.idenity_imported)).check(matches(isDisplayed()));
		onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
		tempQidFile.delete();
		openDrawer(R.id.drawer_layout);
		onView(withId(R.id.select_identity_layout)).check(matches(isDisplayed())).perform(click());
		onView(withText(userToImport)).check(matches(isDisplayed()));


	}

	private void setupData() {
		URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
		mActivity = mActivityTestRule.getActivity();
		mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
		mBoxHelper.bindService(QabelBoxApplication.getInstance());
		mBoxHelper.createTokenIfNeeded(false);
		mBoxHelper.deleteAllIdentities();
		mBoxHelper.addIdentity("spoon");

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

	private void launchExternalIntent(Uri uri) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(uri, "application/json");
		mActivityTestRule.launchActivity(intent);
		mActivity = mActivityTestRule.getActivity();
	}

	@NonNull
	private File createQcoFile(String name, String fileExtension) {
		Identity importUser1 = mBoxHelper.createIdentity(name);
		String exportUser = ContactExportImport.exportIdentityAsContact(importUser1);
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		File file1 = new File(tmpDir, name + "." + fileExtension);
		saveFile(exportUser, file1);
		return file1;
	}


	@NonNull
	private File createQIDFile(String identityName) {
		Identity identity = mBoxHelper.createIdentity(identityName);
		String exportUser = IdentityExportImport.exportIdentity(identity);
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		File file1 = new File(tmpDir, identityName + "." + QabelSchema.FILE_SUFFIX_IDENTITY);
		saveFile(exportUser, file1);
		return file1;
	}

	private void saveFile(String exportUser, File file1) {
		saveJsonIntoFile(exportUser, file1);
		assertNotNull(file1);
		checkFile(file1);
	}

	private void saveJsonIntoFile(String exportUser, File file1) {
		try {
			FileOutputStream fos = new FileOutputStream(file1);
			fos.write(exportUser.getBytes());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			assertNull(e);
		}
	}


}
