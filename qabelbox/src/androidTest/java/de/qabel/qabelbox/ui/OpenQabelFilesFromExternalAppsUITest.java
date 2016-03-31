package de.qabel.qabelbox.ui;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;
import com.squareup.spoon.Spoon;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.config.IdentityExportImport;
import de.qabel.qabelbox.config.QabelSchema;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

public class OpenQabelFilesFromExternalAppsUITest extends UIBoxHelper {
    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, false);
    private MainActivity mActivity;

    public OpenQabelFilesFromExternalAppsUITest() throws IOException {

        setupData();
    }

    @After
    public void cleanUp() {
        unbindService(QabelBoxApplication.getInstance());
    }


    @Test
    public void testOpenQcoFileFromExternal() {
        String userToImport = "contact1";
        File tempQcoFile = createQcoFile(userToImport, QabelSchema.FILE_SUFFIX_CONTACT);
        createIdentityIfNeeded();
        launchExternalIntent(Uri.fromFile(tempQcoFile));
        checkMessageBox();
        tempQcoFile.delete();
        goToContacts();
        Spoon.screenshot(mActivity, "openQCO");
        onView(withText(userToImport)).check(matches(isDisplayed()));

    }


    /**
     * open app with file extension but with ne ready app
     */
    @Test
    public void testOpenQcoSanityFromExternal() {
        String userToImport = "contact1";
        File tempQcoFile = createQcoFile(userToImport, QabelSchema.FILE_SUFFIX_CONTACT);
        deleteAllIdentities();
        launchExternalIntent(Uri.fromFile(tempQcoFile));
        mActivity = mActivityTestRule.getActivity();
        try {
            Spoon.screenshot(UITestHelper.getCurrentActivity(mActivity), "openQCOWithNoIdentity");
        } catch (Throwable throwable) {
            //indicate no error on tested code
        }

        QabelMatcher.matchToolbarTitle(mActivity.getString(R.string.headline_add_identity))
                .check(matches(isDisplayed()));
    }

    /**
     * open a file with unknown extension
     */
    @Test
    public void testOpenUnknownFileTypeFromExternal() {
        String userToImport = "contact1";
        File tempQcoFile = createQcoFile(userToImport, ".unknown");
        createIdentityIfNeeded();
        launchExternalIntent(Uri.fromFile(tempQcoFile));
        onView(withText(R.string.cant_import_file_type_is_unknown)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "openQCO");
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
        tempQcoFile.delete();
    }

    /**
     * open a corrupt contact file
     */
    @Test
    public void testOpenCorruptContactFromExternal() {
        String userToImport = "defectcontaact";
        File tempQcoFile = createQcoFile(userToImport, QabelSchema.FILE_SUFFIX_CONTACT);
        try {
            FileOutputStream fis = new FileOutputStream(tempQcoFile);
            fis.write(1);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            assertNull(e);
        }
        createIdentityIfNeeded();
        launchExternalIntent(Uri.fromFile(tempQcoFile));
        Spoon.screenshot(mActivity, "corruptContact");
        onView(withText(R.string.contact_import_failed)).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
        tempQcoFile.delete();
    }

    /**
     * open a valid identity file
     */
    @Test
    public void testOpenQidFileFromExternal() {
        String userToImport = "identity1.qid";
        File tempQidFile = createQIDFile(userToImport);
        createIdentityIfNeeded();
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

        bindService(QabelBoxApplication.getInstance());
        createTokenIfNeeded(false);
        deleteAllIdentities();
        addIdentity("spoon");

    }

    private void createIdentityIfNeeded() {
        if (getCurrentIdentity() == null) {
            addIdentity("spoon");
        }
    }

    private void checkMessageBox() {
        onView(withText(R.string.dialog_headline_info)).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
    }

    private void goToContacts() {
        openDrawer(R.id.drawer_layout);
        onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
                .perform(click());
        Spoon.screenshot(mActivity, "contacts");
    }

    private void checkFile(File file1) {
        try {
            FileInputStream fis = new FileInputStream(file1);
            assertTrue(file1.exists());
            JSONObject object = new JSONObject(FileHelper.readFileAsText(fis));
            object.length();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            assertNull(e);
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
        Identity importUser1 = createIdentity(name);
        String exportUser = ContactExportImport.exportIdentityAsContact(importUser1);
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File file1 = new File(tmpDir, name + "." + fileExtension);
        saveFile(exportUser, file1);
        return file1;
    }


    @NonNull
    private File createQIDFile(String identityName) {
        Identity identity = createIdentity(identityName);
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
