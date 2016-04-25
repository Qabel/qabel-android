package de.qabel.qabelbox.communication;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetworkInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.RoboApplication;
import de.qabel.qabelbox.communication.callbacks.JsonRequestCallback;
import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Response;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = RoboApplication.class, constants = BuildConfig.class, shadows = {ShadowConnectivityManager.class})
public class OfflineTest {

    private void setConnectivity(Context context, boolean connected) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE);

        ShadowConnectivityManager shadowCM = shadowOf(connectivityManager);

        NetworkInfo disconnectedInfo = ShadowNetworkInfo.newInstance(
                NetworkInfo.DetailedState.DISCONNECTED,
                ConnectivityManager.TYPE_MOBILE,
                ConnectivityManager.TYPE_MOBILE_MMS,
                true,
                connected);
        shadowCM.setNetworkInfo(
                ConnectivityManager.TYPE_MOBILE, disconnectedInfo);
        shadowCM.setActiveNetworkInfo(disconnectedInfo);
        context.sendBroadcast(new Intent(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public Identity createIdentity(String identityName) {
        URI uri = URI.create(QabelBoxApplication.DEFAULT_DROP_SERVER);
        DropServer dropServer = new DropServer(uri, "", true);
        DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator(2 * 8);
        DropURL dropURL = new DropURL(dropServer, adjustableDropIdGenerator);
        Collection<DropURL> dropURLs = new ArrayList<>();
        dropURLs.add(dropURL);

        Identity identity = new Identity(identityName, dropURLs, new QblECKeyPair());
        identity.getPrefixes().add("test");
        return identity;
    }

    @Before
    public void setUp() {
        QabelBoxApplication application = (QabelBoxApplication) RuntimeEnvironment.application;
        application.getService().addIdentity(createIdentity("spoon"));
        AppPreference preference = new AppPreference(application);
        preference.setToken("MAGICFAIRY");
        preference.setAccountName("TestFoo");
        URLs.setBaseBlockURL("http://testing.qabel.de:8888");
    }

    private static final int waitInterval = 100;

    private interface WaitChecker {
        boolean isReady();
    }

    private void waitUntil(long max, WaitChecker checker) {
        try {
            long current = 0;
            while (current < max && !checker.isReady()) {
                Thread.sleep(waitInterval);
                current += waitInterval;
            }
            assertTrue("Wait for Operation failured!", checker.isReady());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    private boolean successIsOffline;

    @Test
    public void testDeviceIsOffline() {
        Context context = RuntimeEnvironment.application;
        PrefixServer testServer = new PrefixServer();

        successIsOffline = false;

        //Disable connectivity
        setConnectivity(context, false);

        //Do Request
        testServer.getPrefix(context, new JsonRequestCallback(new int[]{201}) {
            @Override
            protected void onError(Exception e, @Nullable Response response) {
                fail("Request ended with error");
            }

            @Override
            protected void onJSONSuccess(Response response, JSONObject result) {
                try {
                    assertNotNull(result.getString("prefix"));
                } catch (JSONException e) {
                    fail("Cannot parse JSON");
                }
                successIsOffline = true;
            }
        });

        //Wait a moment
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        //Reenable connectivity and wait for Request to success
        setConnectivity(context, true);

        waitUntil(2000, new WaitChecker() {
            @Override
            public boolean isReady() {
                return successIsOffline == true;
            }
        });
    }

    private boolean successGoOffline;

    @Test
    public void testDeviceGoOffline() {
        Context context = RuntimeEnvironment.application;
        PrefixServer testServer = new PrefixServer();

        successGoOffline = false;

        testServer.getPrefix(context, new JsonRequestCallback(new int[]{201}) {
            @Override
            protected void onError(Exception e, @Nullable Response response) {
                fail("Request ended with error");
            }

            @Override
            protected void onJSONSuccess(Response response, JSONObject result) {
                try {
                    assertNotNull(result.getString("prefix"));
                } catch (JSONException e) {
                    fail("Cannot parse JSON");
                }
                successGoOffline = true;
            }
        });
        //Disable connectivity to interrupt request
        setConnectivity(context, false);

        //Wait a moment
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        //Reenable connectivity
        setConnectivity(context, true);

        waitUntil(2000, new WaitChecker() {
            @Override
            public boolean isReady() {
                return successGoOffline == true;
            }
        });
    }
}
