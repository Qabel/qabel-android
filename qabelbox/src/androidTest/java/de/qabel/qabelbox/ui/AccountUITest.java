package de.qabel.qabelbox.ui;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;

public class AccountUITest extends AbstractUITest {
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
        appPreference.setAccountName(LogoutUITest.ACCOUNT_NAME);
        appPreference.setAccountEMail(LogoutUITest.ACCOUNT_E_MAIL);
    }

}
