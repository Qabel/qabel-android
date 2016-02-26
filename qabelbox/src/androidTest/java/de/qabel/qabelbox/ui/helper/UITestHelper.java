package de.qabel.qabelbox.ui.helper;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;

import de.qabel.qabelbox.QabelBoxApplication;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

/**
 * Created by danny on 18.01.16.
 */
public class UITestHelper {

	public void deleteAppData() {

		((ActivityManager) QabelBoxApplication.getInstance().getSystemService(Activity.ACTIVITY_SERVICE))
				.clearApplicationUserData();
	}

	public void isToastMessageDisplayed(Activity activity, int textId) {

		onView(withText(textId)).inRoot(withDecorView(not(activity.getWindow().getDecorView()))).check(matches(isDisplayed()));
	}

	public static void sleep(int ms) {

		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void clearAllAppData(Context context) {
		if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
			((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
					.clearApplicationUserData(); // note: it has a return value!
		} else {
			// once minSdkVersion goes above 19 in a few years.
		}
	}

	public static Activity getCurrentActivity(Activity mActivity) throws Throwable {

		final Activity[] activity = new Activity[1];
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {

				activity[0] = ActivityLifecycleMonitorRegistry.getInstance()
						.getActivitiesInStage(Stage.RESUMED).iterator().next();
			}
		});
		while (activity[0] == null) {
			Thread.sleep(200);
		}
		return activity[0];
	}
}
