package de.qabel.qabelbox;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.services.LocalQabelService;

public class QabelBoxApplication extends Application {

    public static final String DEFAULT_DROP_SERVER = "http://localhost";
    private LocalQabelService mService;

    private static QabelBoxApplication mInstance = null;
    public static BoxProvider boxProvider;

    static {
        // Enforce SpongyCastle as JCE provider
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    public BoxProvider getProvider() {

        return boxProvider;
    }

    public static QabelBoxApplication getInstance() {

        return mInstance;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        mInstance = this;
        Intent intent = new Intent(this, LocalQabelService.class);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                LocalQabelService.LocalBinder binder = (LocalQabelService.LocalBinder) service;
                mService = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

                mService = null;
            }
        }, Context.BIND_AUTO_CREATE);
    }
}
