package de.qabel.qabelbox;

import android.content.ServiceConnection;

import org.robolectric.shadows.ShadowLog;

import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.services.RoboLocalQabelService;
import de.qabel.qabelbox.test.TestConstants;

public class RoboApplication extends TestApplication {

    @Override
    public void onCreate() {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        URLs.setBaseAccountingURL(TestConstants.ACCOUNTING_URL);
        new AppPreference(this).setToken(TestConstants.TOKEN);
        super.onCreate();
        ShadowLog.stream = System.out;
    }

    @Override
    void initService() {
        mService = new RoboLocalQabelService();
        mService.onCreate();
    }

    @Override
    public void unbindService(ServiceConnection conn) {
    }

}
