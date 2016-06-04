package de.qabel.qabelbox.storage.server;

import android.support.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.communication.callbacks.JSONModelCallback;
import de.qabel.qabelbox.storage.model.BoxQuota;
import de.qabel.qabelbox.util.TestHelper;
import okhttp3.Response;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class BlockServerTest {

    private static final long DEFAULT_QUOTA = 2147483648L;

    private BlockServer blockServer;

    @Before
    public void setUp() {
        blockServer = new AndroidBlockServer(RuntimeEnvironment.application);
    }

    @Test
    public void testQuota() throws Exception {
        BoxQuota[] quota = new BoxQuota[1];
        blockServer.getQuota(new JSONModelCallback<BoxQuota>() {
            @Override
            protected void onSuccess(Response response, BoxQuota model) {
                quota[0] = model;
            }

            @Override
            protected BoxQuota createModel() {
                return new BoxQuota();
            }

            @Override
            protected void onError(Exception e, @Nullable Response response) {
                //Should not happen
            }
        });
        TestHelper.waitUntil(() -> quota[0] != null, "Error waiting for request!");

        BoxQuota boxQuota = quota[0];
        assertEquals(DEFAULT_QUOTA, boxQuota.getQuota());
    }

}
