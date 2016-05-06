package de.qabel.qabelbox.ui;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.intent.matcher.IntentMatchers.*;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class LogoutUITest {
    public static final String ACCOUNT_NAME = "account_name";
    public static final String ACCOUNT_E_MAIL = "accountmail@example.com";
    IntentsTestRule<MainActivity> mainActivityActivityTestRule =
            new MainActivityWithoutFilesFragmentTestRule();
    private AppPreference appPreference;
    private LocalQabelService service;


    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        appPreference = new AppPreference(context);
        appPreference.clear();
        appPreference.setWelcomeScreenShownAt(1);
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        UIBoxHelper uiBoxHelper = new UIBoxHelper(InstrumentationRegistry.getTargetContext());
        uiBoxHelper.bindService(QabelBoxApplication.getInstance());
        service = uiBoxHelper.getService();
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
        appPreference.setAccountName(ACCOUNT_NAME);
        appPreference.setAccountEMail(ACCOUNT_E_MAIL);
    }

    @Test
	public void testLogout() {
        setAccountPreferences();
        appPreference.setToken(TestConstants.TOKEN);
        mainActivityActivityTestRule.launchActivity(null);
		openDrawer(R.id.drawer_layout);
		onView(withText(R.string.logout))
				.check(matches(isDisplayed()))
				.perform(click());
        Intents.intended(allOf(
                hasFlag(Intent.FLAG_ACTIVITY_CLEAR_TASK),
                hasFlag(Intent.FLAG_ACTIVITY_TASK_ON_HOME),
                hasComponent("de.qabel.qabelbox.activities.CreateAccountActivity")));
        onView(withText(R.string.create_account_login_infos)).check(matches(isDisplayed()));

        assertIdentitiesNotDeleted();
        assertThat("Login token not deleted", appPreference.getToken(), nullValue());
        assertThat("Login name deleted", appPreference.getAccountName(), notNullValue());
        assertThat("Login email deleted", appPreference.getAccountEMail(), notNullValue());
    }

    public void assertIdentitiesNotDeleted() {
        assertThat(service.getIdentities().getIdentities(), not(empty()));
    }

}
