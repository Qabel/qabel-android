package de.qabel.qabelbox.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class QabelAuthenticatorService extends Service {
    private QabelAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuthenticator = new QabelAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
