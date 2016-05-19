package de.qabel.qabelbox.ui.files;


import android.widget.SeekBar;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.FilesSearchResultFragment;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.idling.WaitResourceCallback;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;
import de.qabel.qabelbox.ui.matcher.ToolbarMatcher;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static de.qabel.qabelbox.ui.action.QabelViewAction.setText;

public class FileSearchUITest extends FilesFragmentUITestBase {

    private List<ExampleFile> exampleFiles = Arrays.asList(
            new ExampleFile("testfile 2", new byte[1011]),
            new ExampleFile("red.png", new byte[1]),
            new ExampleFile("green.png", new byte[100]),
            new ExampleFile("blue.png", new byte[1011]),
            new ExampleFile("black_1.png", new byte[1011]),
            new ExampleFile("black_2.png", new byte[1024 * 3]),
            new ExampleFile("white.png", new byte[1011]));

    @Override
    protected void setupData() throws Exception {
        addExampleFiles(identity, exampleFiles);
    }

    @Test
    public void searchNamesTest() throws Throwable  {
        UITestHelper.screenShot(mActivity, "startup");
        testSearch("black", 2);
        testSearch("", 7);
        testSearch("png", 6);
        UITestHelper.screenShot(mActivity, "after");
    }

    @Test
    public void searchFilterTest() throws Exception {
        testSearchWithFilter("", 0, 2048, 6);
        testSearchWithFilter("", 0, 10240, 7);
        testSearchWithFilter("", 9000, 10240, 1);
    }

    @Test
    public void filesChangeSearchTest() throws Exception {

        String text = "";

        //start search
        onView(withId(R.id.action_search)).perform(click());
        onView(withHint(R.string.ab_filesearch_hint)).perform(setText(text), pressImeActionButton());
        closeSoftKeyboard();

        int fileCount = exampleFiles.size();
        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(fileCount)));

        //uploadAndDeleteLocalfile file
        mBoxHelper.uploadFile(identity, "black_3", new byte[1024], "");
        mBoxHelper.waitUntilFileCount(identity, fileCount + 1);

        FilesSearchResultFragment searchResultFragment = (FilesSearchResultFragment) mActivity.getFragmentManager().findFragmentByTag(FilesSearchResultFragment.TAG);
        searchResultFragment.injectIdleCallback(getIdlingResource());

        WaitResourceCallback waitResourceCallback = new WaitResourceCallback();
        getIdlingResource().registerIdleTransitionCallback(waitResourceCallback);

        //Refresh and check new item is visible
        onView(withId(R.id.files_list)).perform(swipeDown());

        //UITestHelper.waitUntil(() -> waitResourceCallback.isDone(), "refresh search files failed");

        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(fileCount + 1)));

        //Back to files
        pressBack();

        testIfFileBrowserDisplayed(fileCount + 1);

        //Reuse callback
        waitResourceCallback.reset();

        //start new search
        text = "black";
        onView(withId(R.id.action_search)).perform(click());
        onView(withHint(R.string.ab_filesearch_hint)).perform(setText(text), pressImeActionButton());
        closeSoftKeyboard();

        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(3)));
    }

    /**
     * test if search result match the given. addition check if file browser displayed after back pressed
     *
     * @param text    search text
     * @param results excepted results
     */
    private void testSearch(String text, int results){
        testSearch(text, results, exampleFiles.size());
    }
    private void testSearch(String text, int results, int rangeCount) {

        onView(withId(R.id.action_search)).perform(click());
        onView(withHint(R.string.ab_filesearch_hint)).perform(setText(text), pressImeActionButton());
        closeSoftKeyboard();

        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(results)));

        pressBack();
        testIfFileBrowserDisplayed(rangeCount);
    }

    private void testIfFileBrowserDisplayed(int count) {
        ToolbarMatcher.matchToolbarTitle(mActivity.getString(R.string.headline_files))
                .check(matches(isDisplayed()));
        onView(withId(R.id.files_list)).check(matches(QabelMatcher.withListSize(count)));
    }

    /**
     * test if search result matches the given. addition check if file browser displayed after back pressed
     *
     * @param text    search text
     * @param results excepted results
     */
    private void testSearchWithFilter(String text, int fileSizeMin, int fileSizeMax, int results) throws Exception {

        onView(withId(R.id.action_search)).perform(click());
        onView(withHint(R.string.ab_filesearch_hint)).perform(setText(text), pressImeActionButton());
        closeSoftKeyboard();

        onView(withId(R.id.action_ok)).check(matches(isDisplayed())).perform(click());
        ((SeekBar) mActivity.findViewById(R.id.sbFileSizeMin)).setProgress(fileSizeMin);
        ((SeekBar) mActivity.findViewById(R.id.sbFileSizeMax)).setProgress(fileSizeMax);

        WaitResourceCallback waitResourceCallback = new WaitResourceCallback();
        getIdlingResource().registerIdleTransitionCallback(waitResourceCallback);
        onView(withId(R.id.action_use_filter)).perform(click());

        onView(withId(R.id.files_list)).
                check(matches(isDisplayed())).
                check(matches(QabelMatcher.withListSize(results)));

        pressBack();
        testIfFileBrowserDisplayed(7);
    }
}

