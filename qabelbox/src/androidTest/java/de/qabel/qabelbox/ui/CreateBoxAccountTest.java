package de.qabel.qabelbox.ui;

/**
 * Created by danny on 05.01.2016.
 */

import android.os.PowerManager;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import com.squareup.picasso.PicassoIdlingResource;
import com.squareup.spoon.Spoon;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.activities.CreateAccountActivity;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
//import static de.qabel.qabelbox.ui.matcher.QabelMatcher.withDrawable;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateBoxAccountTest {

	@Rule
	public ActivityTestRule<CreateAccountActivity> mActivityTestRule = new ActivityTestRule<>(CreateAccountActivity.class, false, true);

	private CreateAccountActivity mActivity;
	private UIBoxHelper mBoxHelper;
	private final boolean mFillAccount = true;
	private PowerManager.WakeLock wakeLock;
	private final PicassoIdlingResource mPicassoIdlingResource = new PicassoIdlingResource();
	private SystemAnimations mSystemAnimations;

	public CreateBoxAccountTest() throws IOException {
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
		UIBoxHelper.clearToken();
		mBoxHelper.bindService(QabelBoxApplication.getInstance());
		mBoxHelper.deleteCurrentIdentity();
		mBoxHelper.removeAllIdentities();


	}

	@Test
	public void testCreateAccount() {

		Spoon.screenshot(mActivity, "startup");
		mPicassoIdlingResource.init(mActivity);
		/*testFile("file1.jpg");
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

		onView(withId(R.id.image)).perform(click());*/
	}


}

