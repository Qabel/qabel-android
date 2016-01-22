package de.qabel.qabelbox.ui;

/**
 * Created by danny on 05.01.2016.
 */

import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.support.test.rule.ActivityTestRule;
import android.widget.SeekBar;
import android.widget.Toast;

import com.squareup.spoon.Spoon;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.StorageSearch;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FileSearchTest {

    private final String TAG = this.getClass().getSimpleName();
    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, true);

    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private final boolean mFillAccount = true;
    private PowerManager.WakeLock wakeLock;

    public FileSearchTest() throws IOException {
        //setup data before MainActivity launched. This avoid the call to create identity
        if (mFillAccount) {
            setupData();
        }
    }

    @After
    public void cleanUp() {

        wakeLock.release();
    }

    @Before
    public void setUp() throws IOException, QblStorageException {

        mActivity = mActivityTestRule.getActivity();
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
    }

    private void setupData() {

        mBoxHelper = new UIBoxHelper(mActivity);
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        Identity identity = mBoxHelper.addIdentity("spoon");
        mBoxHelper.setActiveIdentity(identity);
        uploadTestFiles(mBoxHelper.getCurrentIdentity());
    }

    private void uploadTestFiles(Identity identity) {

        int fileCount = 7;
        mBoxHelper.uploadFile(identity, "testfile 2", new byte[1011], "");
        mBoxHelper.uploadFile(identity, "red.png", new byte[1], "");
        mBoxHelper.uploadFile(identity, "green.png", new byte[100], "");
        mBoxHelper.uploadFile(identity, "blue.png", new byte[1011], "");
        mBoxHelper.uploadFile(identity, "black_1.png", new byte[1011], "");
        mBoxHelper.uploadFile(identity, "black_2.png", new byte[1024 * 10], "");
        mBoxHelper.uploadFile(identity, "white.png", new byte[1011], "");

        mBoxHelper.waitUntilFileCount(fileCount);
    }



    @Test
    public void search1ByNamesTest() {

        Spoon.screenshot(mActivity, "startup");
        testSearch("black", 2);
        testSearch("", 7);
        testSearch("png", 6);
        Spoon.screenshot(mActivity, "after");
    }

    @Test
    public void search2FilterTest() {

        testSearchWithFilter("", 0, 2048, 6, true);
        testSearchWithFilter("", 0, 10240, 7, false);
        testSearchWithFilter("", 9000, 10240, 1, false);
    }
/* // commit out to test jenkins
    @Test
    public void search3CacheTest() throws QblStorageException {

        String text = "";
        int results = 7;

        //start search
        onView(withId(R.id.action_search)).perform(click());
        onView(withHint(R.string.ab_filesearch_hint)).perform(typeText(text), pressImeActionButton());
        closeSoftKeyboard();

        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(results)));
        int fileCount = new StorageSearch(mBoxHelper.mBoxVolume.navigate()).getResults().size();
        Spoon.screenshot(mActivity, "before_upload");

        //upload file
        mBoxHelper.uploadFile(mBoxHelper.getCurrentIdentity(), "black_3", new byte[1024], "");
        mBoxHelper.waitUntilFileCount(fileCount + 1);

        onView(withId(R.id.files_list)).perform(swipeDown());
        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(fileCount + 1)));
        Spoon.screenshot(mActivity, "after_refresh");
        pressBack();
        testIfFileBrowserDisplayed(fileCount);

        //start new search
        text = "black";
        onView(withId(R.id.action_search)).perform(click());
        onView(withHint(R.string.ab_filesearch_hint)).perform(typeText(text), pressImeActionButton());
        closeSoftKeyboard();

        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(3)));
        Spoon.screenshot(mActivity, "after_research");
        mBoxHelper.deleteFile(mBoxHelper.getCurrentIdentity(), "black_3","");
    }*/

    /**
     * test if search result match the given. addition check if file browser displayed after back pressed
     *
     * @param text    search text
     * @param results excepted results
     */
    private void testSearch(String text, int results) {

        onView(withId(R.id.action_search)).perform(click());
        onView(withHint(R.string.ab_filesearch_hint)).perform(typeText(text), pressImeActionButton());
        closeSoftKeyboard();

        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(results)));
        Spoon.screenshot(mActivity, "results_" + text);
        pressBack();
        testIfFileBrowserDisplayed(7);
    }

    private void testIfFileBrowserDisplayed(int count) {

        QabelMatcher.matchToolbarTitle(mActivity.getString(R.string.headline_files))
                .check(matches(isDisplayed()));
        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(count)));
    }

    /**
     * test if search result matches the given. addition check if file browser displayed after back pressed
     *
     * @param text    search text
     * @param results excepted results
     */
    private void testSearchWithFilter(String text, int fileSizeMin, int fileSizeMax, int results, boolean screenShot) {

        onView(withId(R.id.action_search)).perform(click());
        onView(withHint(R.string.ab_filesearch_hint)).perform(typeText(text), pressImeActionButton());
        closeSoftKeyboard();
        UITestHelper.sleep(800);
        onView(withId(R.id.action_ok)).check(matches(isDisplayed())).perform(click());
        ((SeekBar) mActivity.findViewById(R.id.sbFileSizeMin)).setProgress(fileSizeMin);
        ((SeekBar) mActivity.findViewById(R.id.sbFileSizeMax)).setProgress(fileSizeMax);
        if (screenShot) {
            Spoon.screenshot(mActivity, "filter_" + fileSizeMin + "_" + fileSizeMax);
        }
        onView(withId(R.id.action_use_filter)).perform(click());

        onView(withId(R.id.files_list)).
                check(matches(isDisplayed())).
                check(matches(QabelMatcher.withListSize(results)));
        if (screenShot) {
            Spoon.screenshot(mActivity, "filter_result_" + fileSizeMin + "_" + fileSizeMax);
        }

        pressBack();
        testIfFileBrowserDisplayed(7);
    }
}

