package de.qabel.qabelbox.config;

import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.qabel.qabelbox.TestConstants;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class AppPreferenceTest {

    private AppPreference appPreference;

    @Before
    public void setUp() throws Exception {
        appPreference = new AppPreference(InstrumentationRegistry.getTargetContext());
    }

    @After
    public void tearDown() throws Exception {
        appPreference.setToken(TestConstants.TOKEN);
        appPreference.setWelcomeScreenShownAt(1);
    }

    @Test
    public void testDeleteToken() {
        appPreference.setToken("token");
        appPreference.setToken(null);
        assertThat(appPreference.getToken(), nullValue());
    }

    @Test
    public void testClear() {
        appPreference.setToken("token");
        appPreference.clear();
        assertThat(appPreference.getToken(), nullValue());
    }

    @Test
    public void testLogout() {
        appPreference.setToken("token");
        appPreference.setAccountEMail("email");
        appPreference.setAccountName("name");
        appPreference.logout();
        assertThat(appPreference.getToken(), nullValue());
        assertThat(appPreference.getAccountEMail(), equalTo("email"));
        assertThat(appPreference.getAccountName(), equalTo("name"));
    }
}
