package de.qabel.qabelbox.ui;

import android.support.test.espresso.intent.rule.IntentsTestRule;

import org.junit.Test;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.CreateAccountActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

public class LoginUITest extends AccountUITest {

    IntentsTestRule<CreateAccountActivity> activityTestRule =
            new IntentsTestRule<>(CreateAccountActivity.class, true, false);


    @Test
    public void testAccountNameAlreadyInserted() {
        setAccountPreferences();
        activityTestRule.launchActivity(null);
        onView(withId(R.id.et_username)).check(matches(allOf(
                withText(AccountUITest.ACCOUNT_NAME), not(isEnabled()))));
        onView(withId(R.id.et_password)).check(matches(withText("")));

        // No idea why 2 clicks are needed.
        onView(withId(R.id.reset_password)).perform(click());
        onView(withId(R.id.reset_password)).perform(click());

        onView(withId(R.id.et_email)).check(matches(allOf(
                withText(AccountUITest.ACCOUNT_E_MAIL), not(isEnabled()))));
    }
}
