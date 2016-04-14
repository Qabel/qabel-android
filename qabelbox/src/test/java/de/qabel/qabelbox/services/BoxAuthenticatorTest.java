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
public class BoxAuthenticatorTest {

    private Context context;
    private BoxAuthenticator boxAuthenticator;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        boxAuthenticator = new BoxAuthenticator(context);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEditProperties() throws Exception {
        boxAuthenticator.editProperties(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAccount() throws Exception {
        boxAuthenticator.addAccount(null, null, null, null, null);
    }

    @Test
    public void testConfirmCredentials() throws Exception {
        assertNull(boxAuthenticator.confirmCredentials(null, null, null));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAuthToken() throws Exception {
        boxAuthenticator.getAuthToken(null, null, null, null);

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAuthTokenLabel() throws Exception {
        boxAuthenticator.getAuthTokenLabel(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCredentials() throws Exception {
        boxAuthenticator.updateCredentials(null, null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testHasFeatures() throws Exception {
        boxAuthenticator.hasFeatures(null, null, null);

    }
}
