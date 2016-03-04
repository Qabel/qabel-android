package de.qabel.qabelbox;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.concurrent.CountDownLatch;

import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.services.LocalQabelService;

public class QabelBoxApplication extends Application {

    public static final String DEFAULT_DROP_SERVER = "http://localhost";
    private static final String TAG = "QabelBoxApplication";
    private LocalQabelService mService;

    private static QabelBoxApplication mInstance = null;
    public static BoxProvider boxProvider;

    static {
        // Enforce SpongyCastle as JCE provider
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    private ServiceConnection mServiceConnection;


    public BoxProvider getProvider() {

        return boxProvider;
    }

    public static QabelBoxApplication getInstance() {

        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mInstance = this;
        Intent intent = new Intent(this, LocalQabelService.class);
        mServiceConnection = getServiceConnection();
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onTerminate() {
        unbindService(mServiceConnection);
        super.onTerminate();
    }

    @NonNull
    private ServiceConnection getServiceConnection() {

        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalQabelService.LocalBinder binder = (LocalQabelService.LocalBinder) service;
                mService = binder.getService();
                Log.d(TAG, "Service binded");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

                mService = null;
            }
        };
    }

    public LocalQabelService getService() {
        return mService;
    }
}