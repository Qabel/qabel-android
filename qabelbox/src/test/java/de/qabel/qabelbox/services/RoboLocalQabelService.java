package de.qabel.qabelbox.services;

import android.content.Context;
import android.content.SharedPreferences;

import org.robolectric.RuntimeEnvironment;

public class RoboLocalQabelService extends LocalQabelService {

    @Override
    public Context getApplicationContext() {
        return RuntimeEnvironment.application;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return RuntimeEnvironment.application.getSharedPreferences(
                LocalQabelService.class.getCanonicalName(), Context.MODE_PRIVATE);
    }

}
