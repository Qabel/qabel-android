package de.qabel.qabelbox;

import android.content.Context;
import android.content.ServiceConnection;

import org.robolectric.shadows.ShadowLog;

import de.qabel.qabelbox.box.interactor.JdbcPrefix;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.test.TestConstants;

public class RoboApplication extends TestApplication {

    @Override
    public void onCreate() {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        URLs.setBaseAccountingURL(TestConstants.ACCOUNTING_URL);
        new AppPreference(this).setToken(TestConstants.TOKEN);
        super.onCreate();
        ShadowLog.stream = System.out;
        JdbcPrefix.INSTANCE.setJdbcPrefix("jdbc:sqlite:");
    }

    @Override
    protected void installMultiDex(Context base) {
        // not needed for robolectric
    }

    @Override
    public void unbindService(ServiceConnection conn) {
    }

}
