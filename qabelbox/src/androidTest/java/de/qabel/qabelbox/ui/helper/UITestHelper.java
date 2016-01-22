package de.qabel.qabelbox.ui.helper;

import android.app.Activity;
import android.app.ActivityManager;

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
}
