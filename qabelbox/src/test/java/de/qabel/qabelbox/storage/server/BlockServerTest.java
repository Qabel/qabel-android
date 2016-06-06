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
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.storage.data.BoxQuotaJSONAdapter;
import de.qabel.qabelbox.storage.model.BoxQuota;
import de.qabel.qabelbox.util.TestHelper;
import okhttp3.Response;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class BlockServerTest {

    private BlockServer blockServer;

    @Before
    public void setUp() {
        blockServer = new MockBlockServer();
    }

    @Test
    public void testQuota() throws Exception {
        BoxQuota[] quota = new BoxQuota[1];
        blockServer.getQuota(new JSONModelCallback<BoxQuota>(new BoxQuotaJSONAdapter()) {
            @Override
            protected void onSuccess(Response response, BoxQuota model) {
                quota[0] = model;
            }

            @Override
            protected void onError(Exception e, @Nullable Response response) {
                //Should not happen
            }
        });

        BoxQuota boxQuota = quota[0];
        assertEquals(MockBlockServer.SIZE, boxQuota.getSize());
        assertEquals(MockBlockServer.QUOTA, boxQuota.getQuota());
    }

}
