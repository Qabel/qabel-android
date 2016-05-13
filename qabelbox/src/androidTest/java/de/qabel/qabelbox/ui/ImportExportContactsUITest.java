package de.qabel.qabelbox.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.matcher.IntentMatchers;
import android.support.test.espresso.intent.rule.IntentsTestRule;

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
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.exceptions.QblStorageEntityExistsException;
import de.qabel.qabelbox.fragments.ContactFragment;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.matcher.ToolbarMatcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ImportExportContactsUITest {

    private final String TAG = this.getClass().getSimpleName();

    private static final String CONTACT_1 = "contact1";
    private static final String CONTACT_2 = "contact2";
    private static final String CONTACT_3 = "contact3";

    @Rule
    public IntentsTestRule<MainActivity> mActivityTestRule =
            new MainActivityWithoutFilesFragmentTestRule(false);
    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;

    private Identity identity;

    @After
    public void cleanUp() {
        if (wakeLock != null) {
            wakeLock.release();
        }
        if (mSystemAnimations != null) {
            mSystemAnimations.enableAll();
        }
        mBoxHelper.unbindService(QabelBoxApplication.getInstance());
    }

    @Before
    public void setUp() throws Exception {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.createTokenIfNeeded(false);
        mBoxHelper.removeAllIdentities();
        identity = mBoxHelper.addIdentity("spoon123");
        createTestContacts();

        mActivity = mActivityTestRule.launchActivity(null);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }

    private void createTestContacts() throws JSONException, QblStorageEntityExistsException {

        mBoxHelper.setActiveIdentity(identity);
        assertThat(mBoxHelper.getService().getContacts().getContacts().size(), is(0));
        createContact(CONTACT_1);
        createContact(CONTACT_2);
        createContact(CONTACT_3);
        assertThat(mBoxHelper.getService().getContacts().getContacts().size(), is(3));
    }

    private void createContact(String name) throws JSONException, QblStorageEntityExistsException {
        Identity identity = mBoxHelper.createIdentity(name);
        String json = ContactExportImport.exportIdentityAsContact(identity);
        addContact(json);
    }

    private void addContact(String contactJSON) throws JSONException, QblStorageEntityExistsException {
        mBoxHelper.getService().addContact(ContactExportImport.parseContactForIdentity(null, new JSONObject(contactJSON)));
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
    public void testExportContactAsQRCode() {
        goToContacts();

        onView(withId(R.id.contact_list)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(CONTACT_1)), longClick()));
        onView(withText(R.string.ExportAsContactWithQRcode)).check(matches(isDisplayed())).perform(click());
        onView(withText(CONTACT_1)).check(matches(isDisplayed()));
        ToolbarMatcher.matchToolbarTitle(mActivity.getString(R.string.headline_qrcode)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "contactQR");

    }

    @Test
    public void testExportSingleContact() throws Exception {

        String userName = "contact1";
        File file1 = new File(mActivity.getCacheDir(), "testexportcontact");
        goToContacts();

        onView(withId(R.id.contact_list))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(userName)), longClick()));
        Spoon.screenshot(mActivity, "exportOne");
        Intent data = new Intent();
        data.setData(Uri.fromFile(file1));
        ContactFragment contactFragment = (ContactFragment) mActivity.getFragmentManager().findFragmentById(R.id.fragment_container);
        contactFragment.enableDocumentProvider(false);
        final LocalQabelService service = QabelBoxApplication.getInstance().getService();
        Contact contact = service.getContacts().getContacts().iterator().next();
        userName = contact.getAlias();
        contactFragment.exportContact(contact);
        contactFragment.onActivityResult(ContactFragment.REQUEST_EXPORT_CONTACT, Activity.RESULT_OK, data);

        checkMessageBox();

        Contact importedContact = ContactExportImport.parseContactForIdentity(identity, checkFile(file1));
        assertEquals(importedContact.getAlias(), userName);

    }

    @Test
    public void testExportContactToExternal(){
        goToContacts();

        //Open dialog for Contact 1
        onView(withId(R.id.contact_list))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(CONTACT_1)), longClick()));

        //Check contact dialog
        onView(withText(CONTACT_1)).inRoot(isDialog()).check(matches(isDisplayed()));

        //Click Send
        onView(withText(R.string.Send)).inRoot(isDialog()).perform(click());
        //Check Chooser for SendIntent is visible
        intended(IntentMatchers.hasExtra(equalTo(Intent.EXTRA_INTENT),
                        IntentMatchers.hasAction(Intent.ACTION_SEND)));


        Spoon.screenshot(mActivity, "sendContact");
    }

    @Test
    public void testExportManyContact() throws JSONException {

        File file1 = new File(mActivity.getCacheDir(), "testexportallcontact");
        assertNotNull(file1);
        goToContacts();
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        Spoon.screenshot(mActivity, "exportAll");
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
    public void testImportSingleContact() {
        String userToImport = "importUser1";
        Identity importUser1 = mBoxHelper.addIdentity(userToImport);
        String exportUser = ContactExportImport.exportIdentityAsContact(importUser1);
        File file1 = new File(mActivity.getCacheDir(), "testexportallcontact");
        saveJsonIntoFile(exportUser, file1);

        assertNotNull(file1);
        goToContacts();

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


}
