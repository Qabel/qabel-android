package de.qabel.qabelbox.ui;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import org.junit.Before;
import org.junit.Test;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.CreateAccountActivity;
import de.qabel.qabelbox.config.AppPreference;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

public class LoginUITest {

    IntentsTestRule<CreateAccountActivity> activityTestRule =
            new IntentsTestRule<>(CreateAccountActivity.class, true, false);

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        AppPreference appPreference = new AppPreference(context);
        appPreference.setAccountName(AccountUITest.ACCOUNT_NAME);
        appPreference.setAccountEMail(AccountUITest.ACCOUNT_E_MAIL);

        activityTestRule.launchActivity(null);
    }


    @Test
    public void testAccountNameAlreadyInserted() {
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
