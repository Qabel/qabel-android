package de.qabel.qabelbox;

import android.content.ServiceConnection;

import de.qabel.qabelbox.services.RoboLocalQabelService;

public class RoboApplication extends QabelBoxApplication {

    @Override
    void initService() {
        mService = new RoboLocalQabelService();
        mService.onCreate();
    }

    @Override
    public void unbindService(ServiceConnection conn) {
    }

}
