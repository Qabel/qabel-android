package de.qabel.qabelbox;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.encoders.Hex;

import java.security.Security;

import de.qabel.ackack.event.EventEmitter;
import de.qabel.core.config.ResourceActor;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.config.AndroidPersistence;
import de.qabel.qabelbox.config.QblSQLiteParams;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.services.LocalQabelService;

public class QabelBoxApplication extends Application {
    public static final String DEFAULT_DROP_SERVER = "http://localhost";
    private LocalQabelService mService;

    public static BoxProvider boxProvider;

    static {
        // Enforce SpongyCastle as JCE provider
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    public BoxProvider getProvider() {
        return boxProvider;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
