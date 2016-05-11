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

public class AccountUITest {
    protected AppPreference appPreference;

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
