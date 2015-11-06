package de.qabel.qabelbox.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class BoxAuthenticatorService extends Service {
    private BoxAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuthenticator = new BoxAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
