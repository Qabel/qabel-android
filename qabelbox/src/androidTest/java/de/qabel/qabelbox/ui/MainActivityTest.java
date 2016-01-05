package de.qabel.qabelbox.ui;

/**
 * Created by danny on 05.01.2016.
 */

import android.support.test.rule.ActivityTestRule;

import com.squareup.spoon.Spoon;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests for MainActivity.
 */


public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    private MainActivity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
    }


    @Test
    public void testCheckEmptyPassword() {
        Spoon.screenshot(mActivity, "initial_state");
        onView(withId(R.id.buttonOpen)).perform(click());
        isToastMessageDisplayed(R.string.enter_db_password);
        Spoon.screenshot(mActivity, "enter_db_password_toast");
    }


    @Test
    public void testCheckIncorrectPassword() {
        onView(withId(R.id.editTextPassword))
                .perform(typeText("HELLO1"), closeSoftKeyboard());
        onView(withText("foobar")).check(doesNotExist());
        Spoon.screenshot(mActivity, "after_enter_password");
        onView(withId(R.id.buttonOpen)).perform(click());
        onView(withId(R.id.buttonOpen)).check(matches(isDisplayed()));
    }

    public void isToastMessageDisplayed(int textId) {
        onView(withText(textId)).inRoot(withDecorView(not(mActivity.getWindow().getDecorView()))).check(matches(isDisplayed()));
    }

}

