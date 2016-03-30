package de.qabel.qabelbox;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.services.LocalQabelService.LocalBinder;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class QabelBoxApplication extends Application {

    private static final String TAG = "QabelBoxApplication";
    private LocalQabelService mService;
    public static final String DEFAULT_DROP_SERVER = "https://test-drop.qabel.de";

    private static QabelBoxApplication mInstance;
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
                LocalBinder binder = (LocalBinder) service;
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

    /**
     * This application class create the service in onCreate.
     * if the application starts with mainActivity and bypass splash (e.g. share from outside, open app with file browser by mime type)
     * it can be that the activity get service early than the application class. because the service creation is asynchron
     * in same cases the app would crashed if acitivty have service and application not yet.
     * <p/>
     * solution is to store service if early created from activity before from application.
     */
    public void serviceCreatedOutside(LocalQabelService service) {
        if (mService == null) {
            Log.d(TAG, "service createdOutside");
            mService = service;
        }
    }
}
