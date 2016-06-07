package de.qabel.qabelbox.ui;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import org.junit.Test;

import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.persistence.RepositoryFactory;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class LogoutUITest extends AccountUITest {
    public static final String ACCOUNT_NAME = "account_name";
    public static final String ACCOUNT_E_MAIL = "accountmail@example.com";
    IntentsTestRule<MainActivity> mainActivityActivityTestRule =
            new MainActivityWithoutFilesFragmentTestRule();

    @Test
    public void testLogout() throws Exception {
        setAccountPreferences();
        appPreference.setToken(TestConstants.TOKEN);
        mainActivityActivityTestRule.launchActivity(null);
        openDrawer(R.id.drawer_layout);
        onView(withText(R.string.headline_settings)).perform(click());
        onView(withText(R.string.logout))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText(R.string.ok)).perform(click());

        assertIdentitiesNotDeleted();
        appPreference = new AppPreference(mainActivityActivityTestRule.getActivity());
        assertThat("Login token not deleted", appPreference.getToken(), nullValue());
        assertThat("Login name not deleted", appPreference.getAccountName(), nullValue());
        assertThat("Login email not deleted", appPreference.getAccountEMail(), nullValue());
    }

    public void assertIdentitiesNotDeleted() throws Exception {
        RepositoryFactory factory = new RepositoryFactory(
                InstrumentationRegistry.getTargetContext());
        IdentityRepository identityRepository = factory.getIdentityRepository(
                factory.getAndroidClientDatabase());
        assertThat(identityRepository.findAll().getIdentities(), not(empty()));
    }

}
