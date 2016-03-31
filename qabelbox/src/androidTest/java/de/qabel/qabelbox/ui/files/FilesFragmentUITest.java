package de.qabel.qabelbox.ui.files;

import android.content.Intent;
import android.os.PowerManager;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.test.suitebuilder.annotation.MediumTest;

import com.squareup.spoon.Spoon;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.TestConstraints;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.BlockServer;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.views.EditTextFont;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;

/**
 * UI Tests for FilesFragment
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FilesFragmentUITest {

    @Rule
    public IntentsTestRule<MainActivity> mActivityTestRule = new IntentsTestRule<>(MainActivity.class, false, true);

    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private final boolean mFillAccount = true;
    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;

    private Identity testIdentity;
    private Identity testIdentity2;

    private Contact testContact;

    private List<ExampleFile> exampleFiles = Arrays.asList(
            new ExampleFile("testfile 2", new byte[1011]),
            new ExampleFile("red.png", new byte[1]),
            new ExampleFile("green.png", new byte[100]),
            new ExampleFile("black_1.png", new byte[1011]),
            new ExampleFile("black_2.png", new byte[1024 * 10]),
            new ExampleFile("white.png", new byte[1011]),
            new ExampleFile("blue.png", new byte[1011]));

    private class ExampleFile {

        private String name;
        private byte[] data;

        public ExampleFile(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public byte[] getData() {
            return data;
        }

    }

    public FilesFragmentUITest() throws Exception {
        //setup data before MainActivity launched. This avoid the call to create identity
        if (mFillAccount) {
            setupData();
        }
    }



	@Before
	public void setUp() throws IOException, QblStorageException {

		URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
		mActivity = mActivityTestRule.getActivity();
		wakeLock = UIActionHelper.wakeupDevice(mActivity);
		mSystemAnimations = new SystemAnimations(mActivity);
		mSystemAnimations.disableAll();
	}

	@After
	public void cleanUp() {

		for (ExampleFile exampleFile : exampleFiles) {
			mBoxHelper.deleteFile(mActivity, testIdentity, exampleFile.getName(), "");
		}
		mBoxHelper.getService().deleteContact(testContact);
		mBoxHelper.deleteIdentity(testIdentity);
		mBoxHelper.deleteIdentity(testIdentity2);

        wakeLock.release();
        mSystemAnimations.enableAll();
        mBoxHelper.unbindService(QabelBoxApplication.getInstance());
    }

	private void setupData() throws Exception {
		mActivity = mActivityTestRule.getActivity();
		mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
		mBoxHelper.bindService(QabelBoxApplication.getInstance());
		mBoxHelper.createTokenIfNeeded(false);

        testIdentity = mBoxHelper.addIdentity("spoon");
        testIdentity2 = mBoxHelper.addIdentity("spoon2");
        String contactJSON = ContactExportImport.exportIdentityAsContact(testIdentity2);
        try {
            testContact = ContactExportImport.parseContactForIdentity(testIdentity, new JSONObject(contactJSON));
            mBoxHelper.getService().addContact(testContact);
        } catch (Exception e) {
            //TODO Log Cant create testContact!
            throw e;
        }

        uploadTestFiles();
    }

    //Upload the example files
    private void uploadTestFiles() {
        BlockServer bs = new BlockServer();
        for (ExampleFile exampleFile : exampleFiles) {
            mBoxHelper.uploadFile(mBoxHelper.mBoxVolume, exampleFile.getName(), exampleFile.getData(), "");
        }
        mBoxHelper.waitUntilFileCount(exampleFiles.size());
    }
    //TODO may check that correct menu items visible

    @Test
    public void shareFileTest() {
        Spoon.screenshot(mActivity, "startup");
        onView(withText(exampleFiles.get(0).getName())).perform(longClick());

        onView(withText(R.string.ShareToQabelUser)).perform(click());

        //Check labels and spinner are visible.
        onView(withText(R.string.headline_share_to_qabeluser)).check(matches(isDisplayed()));
        onView(withText(R.string.share_to_contact_message)).check(matches(isDisplayed()));
        onView(withId(R.id.spinner_identities)).check(matches(isDisplayed()));

        //Check Contact is Visible
        onView(withText(testContact.getAlias())).check(matches(isDisplayed()));

        onView(withText(R.string.ok)).perform(click());

        UITestHelper.sleep(50);

        //Check progress message
        //XXX Message not belongs to the view
        //onView(withText(R.string.dialog_share_sending_in_progress)).inRoot(withDecorView(not(is(mActivity.getWindow().getDecorView())))).check(matches(isDisplayed()));

        UITestHelper.sleep(TestConstraints.SIMPLE_SERVER_ACTION_TIMEOUT);

        //Check success message
        onView(withText(R.string.messsage_file_shared)).inRoot(withDecorView(not(is(mActivity.getWindow().getDecorView())))).check(matches(isDisplayed()));

        Spoon.screenshot(mActivity, "after");
    }

    @Test
    public void sendFileTest() {
        Spoon.screenshot(mActivity, "startup");
        onView(withText(exampleFiles.get(0).getName())).perform(longClick());

        onView(withText(R.string.Send)).perform(click());

        UITestHelper.sleep(1000);

        //Check Chooser
        intended(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(Intent.EXTRA_TITLE, mActivity.getString(R.string.share_via))));
    }

	@Test
	@MediumTest
	public void testCreateFolder() {
		String testFolderName = "A new folder is born";
		performCreateFolder(testFolderName);
		onView(withText(testFolderName))
			.check(matches(isDisplayed()))
			.check(matches(isDescendantOfA(withId(R.id.files_list))));

	}


	@Ignore
	@Test
	public void testCreateFolderNameConflict() {
		fail("Expected behavior not specified yet");
	}

    @Ignore
    @Test
    public void testCreateFolderEmptyName() {
        fail("Expected behavior not specified yet");
    }


	@Test
	public void testRenameFolder() {
		String nameBefore = "A new folder is born";
		String nameAfter = StringUtils.reverse(nameBefore);
		performCreateFolder(nameBefore);
		performRenameFolder(nameBefore, nameAfter);
		onView(withText(nameAfter))
			.check(matches(isDisplayed()))
			.check(matches(isDescendantOfA(withId(R.id.files_list))));
		onView(withText(nameBefore))
			.check(doesNotExist());
	}


    @Test
	public void testRenameFolderNameConflict() {
		String nameBefore = "A new folder is born";
		String nameAfter = StringUtils.reverse(nameBefore);
		performCreateFolder(nameBefore);
		performCreateFolder(nameAfter);
		performRenameFolder(nameBefore, nameAfter);
		onView(withText(R.string.error_folder_already_exists))
			.inRoot(isDialog())
			.check(matches(isDisplayed()));
		onView(withText(R.string.ok))
			.check(matches(isDisplayed()))
			.perform(click());
		onView(withText(nameBefore))
			.check(matches(isDisplayed()))
			.check(matches(isDescendantOfA(withId(R.id.files_list))));
		onView(withText(nameAfter))
			.check(matches(isDisplayed()))
			.check(matches(isDescendantOfA(withId(R.id.files_list))));
	}

	private void performCreateFolder(String name) {
		onView(withId(R.id.fab)).check(matches(isDisplayed())).perform(click());
		onView(withText(R.string.create_folder)).check(matches(isDisplayed())).perform(click());
		onView(withClassName(containsString(EditTextFont.class.getSimpleName())))
			.check(matches(isDisplayed()))
			.perform(replaceText(name));
		onView(withText(R.string.ok))
			.check(matches(isDisplayed()))
			.perform(click());

	}

	private void performRenameFolder(String orig, String result) {
		onView(withText(orig))
			.check(matches(isDisplayed()))
			.perform(longClick());
		onView(withText(R.string.rename_folder))
			.check(matches(isDisplayed()))
			.perform(click());
		onView(withClassName(containsString(EditTextFont.class.getSimpleName())))
			.check(matches(isDisplayed()))
			.perform(replaceText(result));
		onView(withText(R.string.ok))
			.check(matches(isDisplayed()))
			.perform(click());

	}

	@Test
	public void testCreateFolder() {
		fail("Nor implementent, yet");
	}


	@Test
	public void testCreateFolderNameConflict() {
		fail("Nor implementent, yet");
	}


	@Test
	public void testRenameFolder() {
		fail("Nor implementent, yet");
	}

	@Test
	public void testRenameFolderNameConflict() {
		fail("Nor implementent, yet");
	}

}

