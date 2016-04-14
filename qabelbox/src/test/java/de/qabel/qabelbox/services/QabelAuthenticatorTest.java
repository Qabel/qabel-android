package de.qabel.qabelbox.services;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;

import static org.junit.Assert.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class QabelAuthenticatorTest {

    private Context context;
    private QabelAuthenticator qabelAuthenticator;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        qabelAuthenticator = new QabelAuthenticator(context);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEditProperties() throws Exception {
        qabelAuthenticator.editProperties(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAccount() throws Exception {
        qabelAuthenticator.addAccount(null, null, null, null, null);
    }

    @Test
    public void testConfirmCredentials() throws Exception {
        assertNull(qabelAuthenticator.confirmCredentials(null, null, null));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAuthToken() throws Exception {
        qabelAuthenticator.getAuthToken(null, null, null, null);

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAuthTokenLabel() throws Exception {
        qabelAuthenticator.getAuthTokenLabel(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCredentials() throws Exception {
        qabelAuthenticator.updateCredentials(null, null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testHasFeatures() throws Exception {
        qabelAuthenticator.hasFeatures(null, null, null);

    }
}
