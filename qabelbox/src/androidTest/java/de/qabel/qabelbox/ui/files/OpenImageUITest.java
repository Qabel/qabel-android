package de.qabel.qabelbox.ui.files;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import com.squareup.picasso.PicassoIdlingResource;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
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

public class OpenImageUITest extends FilesFragmentUITestBase {

    private PicassoIdlingResource mPicassoIdlingResource;

    private List<ExampleFile> exampleFiles = null;

    private ExampleFile createImageExampleFile(Context context, String filename, Bitmap.CompressFormat format, int id) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(100 * 1024);
        bitmap.compress(format, 100, byteStream);
        byte[] data = new byte[byteStream.size()];
        System.arraycopy(byteStream.toByteArray(), 0, data, 0, byteStream.size());
        return new ExampleFile(filename, data);
    }

    @Override
    protected void setupData() throws Exception {
        if (exampleFiles == null) {
            exampleFiles = Arrays.asList(new ExampleFile("defect.png", new byte[100]),
                    createImageExampleFile(QabelBoxApplication.getInstance(), "file1.jpg", Bitmap.CompressFormat.JPEG, R.drawable.splash_logo),
                    createImageExampleFile(QabelBoxApplication.getInstance(), "file2.png", Bitmap.CompressFormat.PNG, R.drawable.splash_logo),
                    createImageExampleFile(QabelBoxApplication.getInstance(), "file3.png", Bitmap.CompressFormat.PNG, R.drawable.qabel_logo));
        }
        addExampleFiles(identity, exampleFiles);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPicassoIdlingResource = new PicassoIdlingResource();
        Espresso.registerIdlingResources(mPicassoIdlingResource);
        mPicassoIdlingResource.init(mActivity);
        ActivityLifecycleMonitorRegistry
                .getInstance()
                .addLifecycleCallback(mPicassoIdlingResource);
    }

    @Override
    public void cleanUp() {
        Espresso.unregisterIdlingResources(mPicassoIdlingResource);
        ActivityLifecycleMonitorRegistry
                .getInstance()
                .removeLifecycleCallback(mPicassoIdlingResource);
        super.cleanUp();
    }

    @Test
    public void testOpenFiles() throws Throwable {

        mPicassoIdlingResource.init(mActivity);
        testFile("file2.png");
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

