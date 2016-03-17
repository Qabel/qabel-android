package de.qabel.qabelbox.ui;

import android.graphics.Bitmap;
import android.os.PowerManager;
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
import de.qabel.qabelbox.ui.helper.DocumentIntender;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;

import static android.test.MoreAsserts.assertNotEmpty;

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
	private Identity user1, user2;
	private String contact1Json, contact2Json;
	private String TAG = this.getClass().getSimpleName();

	DocumentIntender intender = new DocumentIntender();

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
	public void testExportContact() {
		Spoon.screenshot(mActivity, "exportOne");
		File file1 = new File(mActivity.getCacheDir(), "testexportcontact");
		intender.handleAddFileIntent(file1);
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
