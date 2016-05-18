package de.qabel.qabelbox.ui.files;

/**
 * Created by danny on 05.01.2016.
 */

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.PowerManager;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import com.squareup.picasso.PicassoIdlingResource;
import com.squareup.spoon.Spoon;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static de.qabel.qabelbox.ui.matcher.QabelMatcher.withDrawable;
import static org.hamcrest.core.AllOf.allOf;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenImageUITest {

    @Rule
    public IntentsTestRule<MainActivity> mActivityTestRule = new IntentsTestRule<MainActivity>(
            MainActivity.class, false, false);

    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private PowerManager.WakeLock wakeLock;
    private final PicassoIdlingResource mPicassoIdlingResource = new PicassoIdlingResource();
    private SystemAnimations mSystemAnimations;


    @After
    public void cleanUp() {

        if (wakeLock != null) {
            wakeLock.release();
        }
        if (mSystemAnimations != null) {
            mSystemAnimations.enableAll();
        }
        Espresso.unregisterIdlingResources(mPicassoIdlingResource);
        mBoxHelper.unbindService(QabelBoxApplication.getInstance());
    }

    @Before
    public void setUp() throws IOException, QblStorageException {

        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.removeAllIdentities();
        mBoxHelper.addIdentity("spoon");
        uploadTestFiles();
        mActivity = mActivityTestRule.launchActivity(null);
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        Espresso.registerIdlingResources(mPicassoIdlingResource);
        ActivityLifecycleMonitorRegistry
                .getInstance()
                .addLifecycleCallback(mPicassoIdlingResource);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }

    private void uploadTestFiles() {

        int fileCount = 4;
        mBoxHelper.uploadFile(mBoxHelper.mBoxVolume, "defect.png", new byte[100], "");
        mBoxHelper.uploadDrawableFile(mBoxHelper.mBoxVolume, "file1.jpg", Bitmap.CompressFormat.JPEG, R.drawable.splash_logo);
        mBoxHelper.uploadDrawableFile(mBoxHelper.mBoxVolume, "file2.png", Bitmap.CompressFormat.PNG, R.drawable.splash_logo);
        mBoxHelper.uploadDrawableFile(mBoxHelper.mBoxVolume, "file3.png", Bitmap.CompressFormat.PNG, R.drawable.qabel_logo);

        mBoxHelper.waitUntilFileCount(fileCount);
    }

    @Test
    public void testOpenFiles() throws Throwable {

        mPicassoIdlingResource.init(mActivity);
        testFile("file2.png");
        UITestHelper.screenShot(mActivity, "open_png");
        Instrumentation.ActivityResult activityResult = new Instrumentation.ActivityResult(
                Activity.RESULT_OK, new Intent());
        Matcher<Intent> expectedIntent = hasAction(Intent.ACTION_CHOOSER);
        intending(expectedIntent).respondWith(activityResult);
        onView(withId(R.id.action_imageviewer_open)).perform(click());
        intended(expectedIntent);

        onView(withId(R.id.image)).perform(click());
    }

    @Test
    public void testDefectFiles() throws Throwable {
        mPicassoIdlingResource.init(mActivity);
        testFile("defect.png");
        onView(withDrawable(R.drawable.message_alert_white)).check(matches(isDisplayed()));
        UITestHelper.screenShot(mActivity, "open_defect_file");
    }

    private void testFile(String file) {

        onView(withId(R.id.files_list))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(file)), click()));
        onView(withId(R.id.image)).check(matches(isDisplayed()));
        onView(withId(R.id.action_imageviewer_edit)).check(matches(isDisplayed()));
        onView(withId(R.id.action_imageviewer_open)).check(matches(isDisplayed()));
    }
}

