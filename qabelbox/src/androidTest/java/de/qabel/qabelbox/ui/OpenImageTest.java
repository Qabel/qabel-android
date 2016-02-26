package de.qabel.qabelbox.ui;

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
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;
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

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;

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
//import static de.qabel.qabelbox.ui.matcher.QabelMatcher.withDrawable;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenImageTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, true);

    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private final boolean mFillAccount = true;
    private PowerManager.WakeLock wakeLock;
    private final PicassoIdlingResource mPicassoIdlingResource = new PicassoIdlingResource();
    private SystemAnimations mSystemAnimations;

    public OpenImageTest() throws IOException {
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
    }

    @Before
    public void setUp() throws IOException, QblStorageException {

        mActivity = mActivityTestRule.getActivity();
        Espresso.registerIdlingResources(mPicassoIdlingResource);
        ActivityLifecycleMonitorRegistry
                .getInstance()
                .addLifecycleCallback(mPicassoIdlingResource);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }

    private void setupData() {

        new AppPreference(QabelBoxApplication.getInstance().getApplicationContext()).setToken(QabelBoxApplication.getInstance().getApplicationContext().getString(R.string.blockserver_magic_testtoken));
        mBoxHelper = new UIBoxHelper(mActivity);
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        try {
            Identity old = mBoxHelper.getCurrentIdentity();
            if (old != null) {
                mBoxHelper.deleteIdentity(old);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Identity identity = mBoxHelper.addIdentity(QabelBoxApplication.getInstance().getApplicationContext(),"spoon");
        mBoxHelper.setActiveIdentity(identity);
        uploadTestFiles();
    }

    private void uploadTestFiles() {

        int fileCount = 4;
        mBoxHelper.uploadFile(mBoxHelper.mBoxVolume, "defect.png", new byte[100], "");
        mBoxHelper.uploadDrawableFile(mBoxHelper.mBoxVolume, "file1.jpg", Bitmap.CompressFormat.JPEG, R.drawable.big_logo);
        mBoxHelper.uploadDrawableFile(mBoxHelper.mBoxVolume, "file2.png", Bitmap.CompressFormat.PNG, R.drawable.big_logo);
        mBoxHelper.uploadDrawableFile(mBoxHelper.mBoxVolume, "file3.png", Bitmap.CompressFormat.PNG, R.drawable.qabel_logo);

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
        Instrumentation.ActivityResult activityResult = new Instrumentation.ActivityResult(
                Activity.RESULT_OK, new Intent());
        Intents.init();
        Matcher<Intent> expectedIntent = allOf(hasAction(Intent.ACTION_CHOOSER));
        intending(expectedIntent).respondWith(activityResult);
        onView(withId(R.id.view)).perform(click());
        intended(expectedIntent);
        Intents.release();

        onView(withId(R.id.image)).perform(click());
    }

    @Test
    public void testDefectFiles() {
        mPicassoIdlingResource.init(mActivity);
        testFile("defect.png");
        onView(withDrawable(R.drawable.image_loading_error)).check(matches(isDisplayed()));
        Spoon.screenshot(mActivity, "open_defect_file");
    }

    private void testFile(String file) {

        onView(withId(R.id.files_list))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(file)), click()));
        onView(withId(R.id.image)).check(matches(isDisplayed()));
        onView(withId(R.id.edit)).check(matches(isDisplayed()));
        onView(withId(R.id.view)).check(matches(isDisplayed()));
    }
}

