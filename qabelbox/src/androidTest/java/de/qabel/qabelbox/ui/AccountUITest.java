package de.qabel.qabelbox.ui;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;

import de.qabel.qabelbox.config.AppPreference;

public class AccountUITest extends AbstractUITest {
    public static final String ACCOUNT_NAME = "account_name";
    public static final String ACCOUNT_E_MAIL = "account@example.com";
    protected AppPreference appPreference;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        appPreference = new AppPreference(context);
        appPreference.clear();
        appPreference.setWelcomeScreenShownAt(1);
    }

    @After
    public void resetPreferencesForNonIsolatedTests() {
        if (appPreference != null) {
            setAccountPreferences();
        }
    }

    public void setAccountPreferences() {
        appPreference.setAccountName(ACCOUNT_NAME);
        appPreference.setAccountEMail(ACCOUNT_E_MAIL);
    }

}
