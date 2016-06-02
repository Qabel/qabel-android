package de.qabel.qabelbox.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.test.espresso.intent.matcher.IntentMatchers;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.fragments.ContactFragment;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.matcher.ToolbarMatcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;

public class ImportExportContactsUITest extends AbstractUITest {

    private final String TAG = this.getClass().getSimpleName();

    private static final String CONTACT_1 = "contact1";
    private static final String CONTACT_2 = "contact2";
    private static final String CONTACT_3 = "contact3";

    @Override
    public void setUp() throws Throwable {
        super.setUp();
        System.out.println("CONTACTS: " + contactRepository.find(identity).getContacts().size());
        createTestContacts();
        System.out.println("CONTACTS2: " + contactRepository.find(identity).getContacts().size());

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra(MainActivity.START_CONTACTS_FRAGMENT, true);
        intent.putExtra(MainActivity.START_FILES_FRAGMENT, false);
        intent.putExtra(MainActivity.ACTIVE_IDENTITY, identity.getKeyIdentifier());
        launchActivity(intent);
    }

    private void createTestContacts() throws Throwable {
        createContact(CONTACT_1);
        createContact(CONTACT_2);
        createContact(CONTACT_3);
    }

    private void createContact(String name) throws Throwable {
        contactRepository.save(mBoxHelper.createContact(name), identity);
    }

    private void checkMessageBox() {
        onView(withText(R.string.dialog_headline_info)).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).check(matches(isDisplayed())).perform(click());
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
    public void testExportContactAsQRCode() throws Throwable {
        onView(withText(CONTACT_1)).check(matches(isDisplayed())).perform(longClick());
        UITestHelper.screenShot(mActivity, "longClickOnContact");
        onView(withText(R.string.ExportAsContactWithQRcode)).check(matches(isDisplayed())).perform(click());
        ToolbarMatcher.matchToolbarTitle(mActivity.getString(R.string.headline_qrcode)).check(matches(isDisplayed()));
        UITestHelper.screenShot(mActivity, "contactQR");

    }

    @Test
    public void testExportSingleContact() throws Throwable {

        String userName = "contact1";
        File file1 = new File(mActivity.getCacheDir(), "testexportcontact");
        onView(withText(userName)).perform(longClick());
        UITestHelper.screenShot(mActivity, "exportOne");
        Intent data = new Intent();
        data.setData(Uri.fromFile(file1));
        ContactFragment contactFragment = (ContactFragment) mActivity.getFragmentManager().findFragmentById(R.id.fragment_container);
        contactFragment.enableDocumentProvider(false);
        Contact contact = contactRepository.find(identity).getContacts().iterator().next();
        userName = contact.getAlias();
        contactFragment.exportContact(contact);
        contactFragment.onActivityResult(ContactFragment.REQUEST_EXPORT_CONTACT, Activity.RESULT_OK, data);

        checkMessageBox();

        Contact importedContact = ContactExportImport.parseContactForIdentity(identity, checkFile(file1));
        assertEquals(importedContact.getAlias(), userName);

    }

    @Test
    public void testExportContactToExternal() throws Throwable {
        onView(withText(CONTACT_1)).perform(longClick());
        UITestHelper.screenShot(mActivity, "longClickOnContact");

        //Check contact dialog
        onView(withText(CONTACT_1)).check(matches(isDisplayed()));

        //Click Send
        onView(withText(R.string.Send)).inRoot(isDialog()).perform(click());
        //Check Chooser for SendIntent is visible
        intended(IntentMatchers.hasExtra(equalTo(Intent.EXTRA_INTENT),
                IntentMatchers.hasAction(Intent.ACTION_SEND)));

        UITestHelper.screenShot(mActivity, "sendContact");
    }

    @Ignore
    @Test
    public void testExportManyContact() throws Throwable {

        File file1 = new File(mActivity.getCacheDir(), "testexportallcontact");
        openActionBarOverflowOrOptionsMenu(mContext);
        UITestHelper.screenShot(mActivity, "exportAll");
        Intent data = new Intent();
        data.setData(Uri.fromFile(file1));
        ContactFragment contactFragment = (ContactFragment) mActivity.
                getFragmentManager().findFragmentById(R.id.fragment_container);
        contactFragment.enableDocumentProvider(false);
        contactFragment.exportAllContacts();
        contactFragment.onActivityResult(ContactFragment.REQUEST_EXPORT_CONTACT, Activity.RESULT_OK, data);
        ContactExportImport.ContactsParseResult contact =
                ContactExportImport.parseContactsForIdentity(identity, checkFile(file1));
        assertEquals(contact.getContacts().getContacts().size(), 3);
    }

    @Test
    public void testImportSingleContact() throws Exception {
        String userToImport = "importUser1";
        Identity importUser1 = mBoxHelper.addIdentity(userToImport);
        String exportUser = ContactExportImport.exportIdentityAsContact(importUser1);
        File file1 = new File(mActivity.getCacheDir(), "testexportallcontact");
        saveJsonIntoFile(exportUser, file1);

        assertNotNull(file1);

        Intent data = new Intent();
        data.setData(Uri.fromFile(file1));
        ContactFragment contactFragment = (ContactFragment) mActivity.
                getFragmentManager().findFragmentById(R.id.fragment_container);
        contactFragment.enableDocumentProvider(false);
        contactFragment.onActivityResult(ContactFragment.REQUEST_IMPORT_CONTACT, Activity.RESULT_OK, data);
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

    @Test
    public void testDeleteContact() throws Throwable {
        assertThat(contactRepository.find(identity).getContacts().size(), equalTo(3));
        //Open dialog for Contact 1
        onView(withText(CONTACT_1)).perform(longClick());

        onView(withText(R.string.Delete)).inRoot(isDialog()).perform(click());
        onView(withText(R.string.yes)).perform(click());
        assertThat(contactRepository.find(identity).getContacts().size(), equalTo(2));
    }


}
