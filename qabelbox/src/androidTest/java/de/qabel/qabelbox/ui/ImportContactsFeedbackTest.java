package de.qabel.qabelbox.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PowerManager;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.contrib.DrawerActions;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.exceptions.QblDropInvalidURL;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.config.ContactExportImportTest;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.ui.action.QabelViewAction;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

/**
 * Created by Jan D.S. Wischweh <mail@wischweh.de> on 07.03.16.
 */
public class ImportContactsFeedbackTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public static final String COOKIEMONSTER_ALIAS = "Cookie Monster";
    public static final String COOKIEMONSTER_PUBLICKEYID = "be14d35443af65a750c941fbd20ea16d678a03ac0f3c3bf42448776252a81234";
    public static final String COOKIEMONSTER_DROP = "https://test-drop.qabel.de/AIXqM7n_hjTfpgPrvsDeWX6dc2Yn4F7OfyCtlX52Zkk";

    public static final String JSON_SINGLE_CONTACT = "{\n" +
            "\t\"public_key\": \"" + COOKIEMONSTER_PUBLICKEYID + "\",\n" +
            "\t\"drop_urls\": [\"" + COOKIEMONSTER_DROP + "\"],\n" +
            "\t\"alias\": \"" + COOKIEMONSTER_ALIAS + "\"\n" +
            "}";

    public static final String JSON_CONTACTLIST_WITH_INVALID_ENTRY = "{\n" +
            "\t\"contacts\": [{\n" +
            "\t\t\"public_key\": \"7c879f241a891938d0be68fbc178ced6f926c95385f588fe8924d0d81a96a32a\",\n" +
            "\t\t\"drop_urls\": [\"https://qdrop.prae.me/APlvHMq05d8ylgp64DW2AHFmdJj2hYDQXJiSnr-Holc\"]\n" +
            "\t}, {\n" +
            "\t\t\"public_key\": \"" + COOKIEMONSTER_PUBLICKEYID + "\",\n" +
            "\t\t\"drop_urls\": [\"" + COOKIEMONSTER_DROP + "\"],\n" +
            "\t\t\"alias\": \"" + COOKIEMONSTER_ALIAS + "\"\n" +
            "\t}]\n" +
            "}";


    public static String DEBUG_TAG = "ImportContactsFeedbackTest";

    private PowerManager.WakeLock wakeLock;
    SystemAnimations mSystemAnimations;
    List<Contact> testContacts = new LinkedList<>();
    Contact cookieMonster;

    public ImportContactsFeedbackTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws URISyntaxException, QblDropInvalidURL {
        wakeLock = UIActionHelper.wakeupDevice(getActivity());
        mSystemAnimations = new SystemAnimations(getActivity());
        mSystemAnimations.disableAll();
        navigateToContacts();
        initTestContacts();

    }

    private void initTestContacts() throws URISyntaxException, QblDropInvalidURL {
        testContacts = ContactExportImportTest.initTestContacts();
        cookieMonster = ContactExportImportTest.initContact(COOKIEMONSTER_ALIAS, COOKIEMONSTER_PUBLICKEYID, COOKIEMONSTER_DROP);

    }

    @After
    public void tearDown() {
        wakeLock.release();
        mSystemAnimations.enableAll();
        removeTestContacts();
    }

    @Test
    @MediumTest
    public void testImportSuccessMany() throws IOException, IntentFilter.MalformedMimeTypeException {
        MainActivity activityAfterImport = instrumentationReturnWithImportJSON(ContactExportImportTest.JSON_CONTACTLIST_TIGERSCLAW);
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.contact_import_successfull_many, 4, 4);
        ViewInteraction dialog = onView(withText(expectedText)).inRoot(isDialog());
        dialog.check(matches(isDisplayed()));
    }

    @Test
    @MediumTest
    public void testImportSuccessOne() throws IOException, IntentFilter.MalformedMimeTypeException {
        MainActivity activityAfterImport = instrumentationReturnWithImportJSON(JSON_SINGLE_CONTACT);
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.contact_import_successfull);
        ViewInteraction dialog = onView(withText(expectedText)).inRoot(isDialog());
        dialog.check(matches(isDisplayed()));
    }

    @Test
    public void testPartialImportSuccess() throws IOException, IntentFilter.MalformedMimeTypeException {
        MainActivity activityAfterImport = instrumentationReturnWithImportJSON(JSON_CONTACTLIST_WITH_INVALID_ENTRY);
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.contact_import_successfull_many, 1, 2);
        ViewInteraction dialog = onView(withText(expectedText)).inRoot(isDialog());
        dialog.check(matches(isDisplayed()));
    }

    @Test
    @MediumTest
    public void testImportFailure() throws IOException, IntentFilter.MalformedMimeTypeException {
        MainActivity activityAfterImport = instrumentationReturnWithImportJSON(ContactExportImportTest.JSON_CONTACTLIST_INVALID_ENTRIES);
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.contact_import_failed);
        ViewInteraction dialog = onView(withText(expectedText)).inRoot(isDialog());
        dialog.check(matches(isDisplayed()));
    }


    private void navigateToContacts() {
        DrawerActions.openDrawer(R.id.drawer_layout);
        onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
                .perform(click());
    }

    private MainActivity instrumentationReturnWithImportJSON(String JSON_FILE_CONTENT) throws IOException, IntentFilter.MalformedMimeTypeException {
        File tmpFile = File.createTempFile("testfile_", ".qco");
        FileUtils.writeStringToFile(tmpFile, JSON_FILE_CONTENT);

        IntentFilter pickFilter = new IntentFilter(Intent.ACTION_OPEN_DOCUMENT, "*/*");
        pickFilter.addCategory(Intent.CATEGORY_OPENABLE);

        Intent returnIntent = new Intent();
        returnIntent.setData(Uri.fromFile(tmpFile));

        Instrumentation.ActivityResult activityResult = new Instrumentation.ActivityResult(Activity.RESULT_OK, returnIntent);
        Instrumentation.ActivityMonitor returnTheTmpFileMonitor = new Instrumentation.ActivityMonitor(pickFilter, activityResult, true);
        getInstrumentation().addMonitor(returnTheTmpFileMonitor);
        onView(withId(R.id.fab)).perform(ViewActions.click());
        UITestHelper.sleep(1000);
        onView(withText(R.string.from_file)).inRoot(isDialog()).perform(ViewActions.click());
        return (MainActivity) getInstrumentation().waitForMonitorWithTimeout(returnTheTmpFileMonitor, 5);
    }

    private void removeTestContacts() {
        LocalQabelService service = QabelBoxApplication.getInstance().getService();
        for (Contact contact : testContacts) {
            service.deleteContact(contact);
        }
        service.deleteContact(cookieMonster);
    }


}