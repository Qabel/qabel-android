package de.qabel.qabelbox;

import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;

import org.robolectric.RuntimeEnvironment;

import de.qabel.qabelbox.services.LocalQabelService;

public class RoboApplication extends QabelBoxApplication {

    @Override
    void initService() {
        mService = new LocalQabelService() {

            @Override
            public Context getApplicationContext() {
                return RuntimeEnvironment.application;
            }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return RuntimeEnvironment.application.getSharedPreferences(
                        LocalQabelService.class.getCanonicalName(), Context.MODE_PRIVATE);
            }

        };
        mService.onCreate();
    }

    @Override
    public void unbindService(ServiceConnection conn) {
    }
}
