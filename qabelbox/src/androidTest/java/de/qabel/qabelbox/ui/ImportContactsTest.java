package de.qabel.qabelbox.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PowerManager;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.config.ContactExportImportTest;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by Jan D.S. Wischweh <mail@wischweh.de> on 07.03.16.
 */
public class ImportContactsTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private PowerManager.WakeLock wakeLock;
    SystemAnimations mSystemAnimations;


    public ImportContactsTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() {
        wakeLock = UIActionHelper.wakeupDevice(getActivity());
        mSystemAnimations = new SystemAnimations(getActivity());
        mSystemAnimations.disableAll();

    }

    @After
    public void tearDown() {
        wakeLock.release();
        mSystemAnimations.enableAll();
    }

    @Test
    @MediumTest
    public void testImportSuccessMany() throws IOException, IntentFilter.MalformedMimeTypeException {
        MainActivity activityAfterImport = instrumentationReturnWithImportJSON(ContactExportImportTest.JSON_CONTACTLIST_TIGERSCLAW);
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.contact_import_successfull_many, new String[]{"4", "4"});
        ViewInteraction dialog = onView(withText(expectedText)).inRoot(isDialog());
        dialog.check(matches(isDisplayed()));
    }

    @Test
    @MediumTest
    public void testImportSuccessOne() throws IOException, IntentFilter.MalformedMimeTypeException {
        MainActivity activityAfterImport = instrumentationReturnWithImportJSON(ContactExportImportTest.JSON_SINGLE_CONTACT);
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.contact_import_successfull);
        ViewInteraction dialog = onView(withText(expectedText)).inRoot(isDialog());
        dialog.check(matches(isDisplayed()));
    }

    @Test
    public void testPartialImportSuccess() throws IOException, IntentFilter.MalformedMimeTypeException {
        MainActivity activityAfterImport = instrumentationReturnWithImportJSON(ContactExportImportTest.JSON_CONTACTLIST_WITH_INVALID_ENTRY);
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.contact_import_successfull_many, new String[]{"1", "2"});
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


    private MainActivity instrumentationReturnWithImportJSON(String JSON_FILE_CONTENT) throws IOException, IntentFilter.MalformedMimeTypeException {
        File tmpFile = File.createTempFile("testfile_", ".qco");
        FileUtils.writeStringToFile(tmpFile, JSON_FILE_CONTENT);

        IntentFilter pickFilter = new IntentFilter(Intent.ACTION_OPEN_DOCUMENT, "*/*");
        pickFilter.addCategory(Intent.CATEGORY_OPENABLE);

        Intent returnIntent = new Intent();
        returnIntent.setData(Uri.fromFile(tmpFile));

        Instrumentation.ActivityResult activityResult = new Instrumentation.ActivityResult(Activity.RESULT_OK, returnIntent);
        Instrumentation.ActivityMonitor returnTheTmpFileMobitor = new Instrumentation.ActivityMonitor(pickFilter, activityResult, true);
        getInstrumentation().addMonitor(returnTheTmpFileMobitor);
        onView(withId(R.id.fab)).perform(ViewActions.click());
        onView(withId(R.id.add_contact_from_file)).perform(ViewActions.click());
        return (MainActivity) getInstrumentation().waitForMonitorWithTimeout(returnTheTmpFileMobitor, 5);
    }




}
