package de.qabel.qabelbox.ui.helper;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingPolicy;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.ViewInteraction;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.squareup.spoon.Spoon;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.SettingsFragment;
import de.qabel.qabelbox.ui.idling.ElapsedTimeIdlingResource;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.not;

public class UITestHelper {

    public static File screenShot(Activity activity, String screenName) throws Throwable {
        int permissionCheck = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            return Spoon.screenshot(getCurrentActivity(activity), screenName);
        } else {
            Log.w("UTTestHelper", "Skipping screenshot because the permission is not granted");
            return null;
        }
    }

    public static void closeKeyboard() {
        closeSoftKeyboard();
        sleep(500);
    }

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
            assertNull(e);
            e.printStackTrace();
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

    public static void disableBugReporting(Context context) {

        SharedPreferences preferences = context.getSharedPreferences(
                SettingsFragment.APP_PREF_NAME,
                Context.MODE_PRIVATE);
        preferences.edit().putBoolean(context.getString(R.string.settings_key_bugreporting_enabled), false).commit();
    }

    public static ViewInteraction waitForView(String text, long waitingTimeMS) {
        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(waitingTimeMS, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(waitingTimeMS, TimeUnit.MILLISECONDS);

        IdlingResource idlingResource = new ElapsedTimeIdlingResource(waitingTimeMS);
        Espresso.registerIdlingResources(idlingResource);
        ViewInteraction element = onView(withText(text));
        Espresso.unregisterIdlingResources(idlingResource);
        return element;
    }

    public static ViewInteraction waitForView(int id, long waitingTimeMS) {

        IdlingPolicy master = IdlingPolicies.getMasterIdlingPolicy();
        IdlingPolicy error = IdlingPolicies.getDynamicIdlingResourceErrorPolicy();

        // Make sure Espresso does not time out

        IdlingPolicies.setMasterPolicyTimeout(waitingTimeMS, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(waitingTimeMS, TimeUnit.MILLISECONDS);

        IdlingResource idlingResource = new ElapsedTimeIdlingResource(waitingTimeMS);
        Espresso.registerIdlingResources(idlingResource);
        ViewInteraction element = onView(withId(id));
        Espresso.unregisterIdlingResources(idlingResource);

        IdlingPolicies.setMasterPolicyTimeout(master.getIdleTimeout(), TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(error.getIdleTimeout(), TimeUnit.SECONDS);
        return element;
    }

    public static void waitUntil(Callable<Boolean> requirement, String message) throws Exception {
        long maximumTime = System.currentTimeMillis() + 10000L;
        long pollInterval = 100L;
        while (System.currentTimeMillis() < maximumTime) {
            if (requirement.call()) {
                return;
            }
            Thread.sleep(pollInterval);
        }
        fail(message);
    }
}
