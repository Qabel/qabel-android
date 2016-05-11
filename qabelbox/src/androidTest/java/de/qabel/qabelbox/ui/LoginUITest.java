package de.qabel.qabelbox.ui;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.CreateAccountActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;

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

    private AppPreference appPreference;


    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        appPreference = new AppPreference(context);
        appPreference.clear();
        appPreference.setWelcomeScreenShownAt(1);
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        UIBoxHelper uiBoxHelper = new UIBoxHelper(InstrumentationRegistry.getTargetContext());
        uiBoxHelper.bindService(QabelBoxApplication.getInstance());
        uiBoxHelper.createTokenIfNeeded(false);
        uiBoxHelper.removeAllIdentities();
        uiBoxHelper.addIdentity("identity");
    }

    @After
    public void tearDown() {
        if (appPreference != null) {
            // Because other tests are probably not correctly isolated
            setAccountPreferences();
        }
    }

    public void setAccountPreferences() {
        appPreference.setAccountName(LogoutUITest.ACCOUNT_NAME);
        appPreference.setAccountEMail(LogoutUITest.ACCOUNT_E_MAIL);
    }

    @Test
    public void testAccountNameAlreadyInserted() {
        setAccountPreferences();
        activityTestRule.launchActivity(null);
        onView(withId(R.id.et_username)).check(matches(allOf(
                withText(LogoutUITest.ACCOUNT_NAME), not(isEnabled()))));
        onView(withId(R.id.et_password)).check(matches(withText("")));

        // No idea why 2 clicks are needed.
        onView(withId(R.id.reset_password)).perform(click());
        onView(withId(R.id.reset_password)).perform(click());

        onView(withId(R.id.et_email)).check(matches(allOf(
                withText(LogoutUITest.ACCOUNT_E_MAIL), not(isEnabled()))));
    }
}
