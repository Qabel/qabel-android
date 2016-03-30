package de.qabel.qabelbox.ui;


import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityResult;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import com.squareup.picasso.PicassoIdlingResource;
import com.squareup.spoon.Spoon;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.drawable;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import org.hamcrest.Matcher;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static de.qabel.qabelbox.ui.matcher.QabelMatcher.withDrawable;
import static org.hamcrest.core.AllOf.allOf;
//import static de.qabel.qabelbox.ui.matcher.QabelMatcher.withDrawable;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenImageUITest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, true);

    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private final boolean mFillAccount = true;
    private WakeLock wakeLock;
    private final PicassoIdlingResource mPicassoIdlingResource = new PicassoIdlingResource();
    private SystemAnimations mSystemAnimations;

    public OpenImageUITest() throws IOException {
        //setup data before MainActivity launched. This avoid the call to create identity
        if (mFillAccount) {
            setupData();
        }
    }

    @After
    public void cleanUp() {

        wakeLock.release();
        Espresso.unregisterIdlingResources(mPicassoIdlingResource);
        mSystemAnimations.enableAll();
        mBoxHelper.unbindService(QabelBoxApplication.getInstance());
    }

    @Before
    public void setUp() throws IOException, QblStorageException {

        mActivity = mActivityTestRule.getActivity();
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        Espresso.registerIdlingResources(mPicassoIdlingResource);
        ActivityLifecycleMonitorRegistry
                .getInstance()
                .addLifecycleCallback(mPicassoIdlingResource);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }

    private void setupData() {
        mActivity = mActivityTestRule.getActivity();
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.createTokenIfNeeded(false);
        try {
            Identity old = mBoxHelper.getCurrentIdentity();
            if (old != null) {
                mBoxHelper.deleteIdentity(old);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBoxHelper.removeAllIdentities();
        mBoxHelper.addIdentity("spoon");
        uploadTestFiles();
    }

    private void uploadTestFiles() {

        int fileCount = 4;
        mBoxHelper.uploadFile(mBoxHelper.mBoxVolume, "defect.png", new byte[100], "");
        mBoxHelper.uploadDrawableFile(mBoxHelper.mBoxVolume, "file1.jpg", CompressFormat.JPEG, drawable.splash_logo);
        mBoxHelper.uploadDrawableFile(mBoxHelper.mBoxVolume, "file2.png", CompressFormat.PNG, drawable.splash_logo);
        mBoxHelper.uploadDrawableFile(mBoxHelper.mBoxVolume, "file3.png", CompressFormat.PNG, drawable.qabel_logo);

        mBoxHelper.waitUntilFileCount(fileCount);
    }

    @Test
    public void testOpenFiles() {

        Spoon.screenshot(mActivity, "startup");
        mPicassoIdlingResource.init(mActivity);
        testFile("file1.jpg");
        Spoon.screenshot(mActivity, "open_jpg");
        pressBack();
        testFile("file2.png");
        Spoon.screenshot(mActivity, "open_png");
        ActivityResult activityResult = new ActivityResult(
                Activity.RESULT_OK, new Intent());
        Intents.init();
        Matcher<Intent> expectedIntent = allOf(hasAction(Intent.ACTION_CHOOSER));
        intending(expectedIntent).respondWith(activityResult);
        onView(withId(id.action_imageviewer_open)).perform(click());
        intended(expectedIntent);
        Intents.release();

        onView(withId(id.image)).perform(click());
    }

    @Test
    public void testDefectFiles() {
        mPicassoIdlingResource.init(mActivity);
        testFile("defect.png");
        onView(withDrawable(drawable.image_loading_error)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "open_defect_file");
    }

    private void testFile(String file) {

        onView(withId(id.files_list))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(file)), click()));
        onView(withId(id.image)).check(matches(isDisplayed()));
        onView(withId(id.action_imageviewer_edit)).check(matches(isDisplayed()));
        onView(withId(id.action_imageviewer_open)).check(matches(isDisplayed()));
    }
}

