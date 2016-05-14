package de.qabel.qabelbox.ui.files;

import android.content.Intent;
import android.support.design.internal.NavigationMenuItemView;
import android.support.test.espresso.Espresso;

import com.squareup.spoon.Spoon;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.ui.AbstractUITest;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.idling.InjectedIdlingResource;
import de.qabel.qabelbox.ui.idling.WaitResourceCallback;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;
import de.qabel.qabelbox.ui.matcher.ToastMatcher;
import de.qabel.qabelbox.ui.matcher.ToolbarMatcher;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static de.qabel.qabelbox.ui.action.QabelViewAction.setText;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

public class FilesFragmentUITest extends AbstractUITest {

    private static final String TEST_FOLDER = "Bilder";
    private static final String CREATE_FOLDER_TEST_NAME = "TestDirectory";

    private InjectedIdlingResource idlingResource;

    private Identity testIdentity2;

    private Contact testContact;
    private Contact testContact2;

    private List<ExampleFile> exampleFiles = Arrays.asList(
            new ExampleFile("testfile 2", new byte[1011]),
            new ExampleFile("red.png", new byte[1]),
            new ExampleFile("black_1.png", new byte[1011]),
            new ExampleFile("black_2.png", new byte[1024 * 2]));

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

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setupData();
        launchActivity(new Intent(Intent.ACTION_MAIN));
        idlingResource = new InjectedIdlingResource();
        mActivity.filesFragment.injectIdleCallback(idlingResource);
        Espresso.registerIdlingResources(idlingResource);
    }

    private void setupData() throws Exception {
        testIdentity2 = mBoxHelper.addIdentity("spoon2");

        testContact = new Contact(identity.getAlias(), identity.getDropUrls(), identity.getEcPublicKey());
        testContact2 = new Contact(testIdentity2.getAlias(), testIdentity2.getDropUrls(), testIdentity2.getEcPublicKey());

        mBoxHelper.getService().addContact(testContact);
        mBoxHelper.setActiveIdentity(identity);
        mBoxHelper.getService().addContact(testContact2);

        mBoxHelper.setActiveIdentity(identity);

        mBoxHelper.createFolder(TEST_FOLDER, identity, null);

        for (ExampleFile exampleFile : exampleFiles) {
            mBoxHelper.uploadFile(mBoxHelper.mBoxVolume, exampleFile.getName(), exampleFile.getData(), "");
        }
        mBoxHelper.waitUntilFileCount(exampleFiles.size());
    }

    @Override
    public void cleanUp() {
        Espresso.unregisterIdlingResources(idlingResource);
        super.cleanUp();
    }

    @Test
    @Ignore("instable. to big, split up")
    public void shareFileTest() throws Exception {
        onView(withText(exampleFiles.get(0).getName())).perform(longClick());

        onView(withText(R.string.ShareToQabelUser)).perform(click());

        //Check labels and spinner are visible.
        onView(withText(R.string.headline_share_to_qabeluser)).check(matches(isDisplayed()));
        onView(withText(R.string.share_to_contact_message)).check(matches(isDisplayed()));
        onView(withId(R.id.spinner_identities)).check(matches(isDisplayed()));

        //Check Contact is Visible
        onView(withText(testContact2.getAlias())).check(matches(isDisplayed()));

        WaitResourceCallback waitCallback = new WaitResourceCallback();
        idlingResource.registerIdleTransitionCallback(waitCallback);

        //Perform share
        onView(withText(R.string.ok)).perform(click());

        UITestHelper.waitUntil(() -> waitCallback.isDone(), "Perform share failed!");

       // onView(withText(R.string.dialog_share_sending_in_progress)).inRoot(isDialog()).check(matches(isDisplayed()));

        //Check success message
        onView(withText(R.string.messsage_file_shared)).inRoot(ToastMatcher.isToast()).check(matches(isDisplayed()));

        //Change to testIdentity2
        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText(testIdentity2.getAlias()))).perform(click());

        //Accept share from identity
        openDrawer(R.id.drawer_layout);
        onView(withText(R.string.Contacts)).check(matches(isDisplayed())).perform(click());
        onView(withText(testContact.getAlias())).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.accept_share)).check(matches(isDisplayed())).perform(click());

        //Go to files
        goToFiles();

        //Check Menu disabled for shared folder
        onView(withText(R.string.shared_with_you)).perform(longClick());

        onView(withId(R.id.bs_main)).check(doesNotExist());

        onView(withText(R.string.shared_with_you)).check(matches(isDisplayed())).perform(click());

        //Check shared file is visible in shared files folder
        onData(withText(exampleFiles.get(0).getName())).inAdapterView(withId(R.id.files_list)).check(matches(isDisplayed()));
        containsString(mActivity.getString(R.string.filebrowser_file_is_shared_from).replace("%1", testContact2.getAlias())).matches(isDisplayed());
    }

    private void goToFiles() {
        openDrawer(R.id.drawer_layout);
        onView(withText(R.string.filebrowser)).check(matches(isDisplayed())).perform(click());
    }

    @Test
    public void sendFileTest() {
        Spoon.screenshot(mActivity, "startup");
        onView(withText(exampleFiles.get(0).getName())).perform(longClick());

        onView(withText(R.string.Send)).perform(click());

        //Check Chooser
        intended(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(Intent.EXTRA_TITLE, mActivity.getString(R.string.share_via))));
    }

    private void createFolder() {
        onView(withId(R.id.fab)).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.create_folder)).check(matches(isDisplayed())).perform(click());

        onView(allOf(withClassName(endsWith("EditTextFont")))).perform(setText(CREATE_FOLDER_TEST_NAME));
        onView(withText(R.string.ok)).perform(click());
    }

    @Test
    public void testCreateFolder() {
        //Create testFolder in root
        createFolder();

        //Check list size
        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(exampleFiles.size() + 1)));
        //Check new item exists and click
        onView(withText(CREATE_FOLDER_TEST_NAME)).check(matches(isDisplayed())).perform(click());

        //Empty folder
        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(0)));

        //Create subfolder
        createFolder();

        //Check list size
        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(1)));

        //Check new item exists and click
        onView(withText(CREATE_FOLDER_TEST_NAME)).check(matches(isDisplayed())).perform(click());

        //Check toolbar title and path
        ToolbarMatcher.matchToolbarTitle(mActivity.getString(R.string.headline_files));
        ToolbarMatcher.matchToolbarSubTitle(File.separator + CREATE_FOLDER_TEST_NAME + File.separator + CREATE_FOLDER_TEST_NAME + File.separator);
    }

}

