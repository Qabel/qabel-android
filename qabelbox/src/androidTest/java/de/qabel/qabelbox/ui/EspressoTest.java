package de.qabel.qabelbox.ui;

/**
 * Created by danny on 04.01.2016.
 */

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class EspressoTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void test1IncorrectLogin() {
        // Type text and then press the button. check if

        onView(withId(R.id.editTextPassword))
                .perform(typeText("HELLO1"), closeSoftKeyboard());
        onView(withId(R.id.buttonOpen)).perform(click());
        onView(withId(R.id.buttonOpen)).check(matches(isDisplayed()));
        onView(withText("foobar")).check(doesNotExist());
    }

    @Test
    public void test2CorrectLogin() {
        // Type text and then press the button.
        onView(withId(R.id.editTextPassword))
                .perform(typeText("test"), closeSoftKeyboard());
        onView(withId(R.id.buttonOpen)).perform(click());
        onView(withId(R.id.buttonOpen)).check(doesNotExist());
        onView(withText("foobar")).check(matches(isDisplayed()));
    }

}

