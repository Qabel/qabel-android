package de.qabel.qabelbox.ui.files;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.config.IdentityExportImport;
import de.qabel.qabelbox.config.QabelSchema;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.ui.AbstractUITest;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.util.IdentityHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@Ignore("Until LocalQabelService has been removed")
public class OpenQabelFilesFromExternalAppsUITest extends AbstractUITest {

    @Test
    public void testOpenQcoFileFromExternal() throws Throwable {
        String userToImport = "contact1";
        File tempQcoFile = createQcoFile(userToImport, QabelSchema.FILE_SUFFIX_CONTACT);
        launchExternalIntent(Uri.fromFile(tempQcoFile));
        checkMessageBox();
        tempQcoFile.delete();
        UITestHelper.screenShot(mActivity, "openQCO");
        Iterator<Contact> iterator = contactRepository.find(identity).getContacts().iterator();
        assertTrue("No contact found in contact repository", iterator.hasNext());
        assertEquals("Contact does not have the correct alias",
                iterator.next().getAlias(), userToImport);
    }


    /**
     * open a file with unknown extension
     */
    @Test
    public void testOpenUnknownFileTypeFromExternal() throws Throwable{
        String userToImport = "contact1";
        File tempQcoFile = createQcoFile(userToImport, ".unknown");
        launchExternalIntent(Uri.fromFile(tempQcoFile));
        onView(withText(R.string.cant_import_file_type_is_unknown)).check(matches(isDisplayed()));
        UITestHelper.screenShot(mActivity, "openQCO");
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
        tempQcoFile.delete();
    }

    /**
     * open a corrupt contact file
     */
    @Test
    public void testOpenCorruptContactFromExternal() throws Throwable{
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
        launchExternalIntent(Uri.fromFile(tempQcoFile));
        UITestHelper.screenShot(mActivity, "corruptContact");
        onView(withText(R.string.contact_import_failed)).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
        tempQcoFile.delete();
    }

    /**
     * open a valid identity file
     */
    @Test
    public void testOpenQidFileFromExternal() throws Throwable{
        String userToImport = "identity1.qid";
        Identity identity1 = createIdentity(userToImport);
        String exportUser = IdentityExportImport.exportIdentity(identity1);
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File tempQidFile = new File(tmpDir, userToImport + "." + QabelSchema.FILE_SUFFIX_IDENTITY);
        saveFile(exportUser, tempQidFile);
        launchExternalIntent(Uri.fromFile(tempQidFile));
        onView(withText(R.string.idenity_imported)).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
        tempQidFile.delete();
        Set<Identity> identities = identityRepository.findAll().getIdentities();
        assertThat(identities, hasSize(2));
    }

    private void checkMessageBox() {
        onView(withText(R.string.dialog_headline_info)).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
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
        launchActivity(intent);
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

    private Identity createIdentity(String name) {
        return IdentityHelper.createIdentity(name, null);
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
