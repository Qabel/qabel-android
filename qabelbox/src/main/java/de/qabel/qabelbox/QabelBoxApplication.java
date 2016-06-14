package de.qabel.qabelbox;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import de.qabel.qabelbox.dagger.components.ApplicationComponent;
import de.qabel.qabelbox.dagger.components.DaggerApplicationComponent;
import de.qabel.qabelbox.dagger.modules.ApplicationModule;
import de.qabel.qabelbox.services.LocalQabelService;

public class QabelBoxApplication extends Application {

    private static final String TAG = "QabelBoxApplication";
    LocalQabelService mService;
    public static final String DEFAULT_DROP_SERVER = "https://test-drop.qabel.de";

    static QabelBoxApplication mInstance = null;

    static {
        // Enforce SpongyCastle as JCE provider
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    private ServiceConnection mServiceConnection;

    @Override
    protected void attachBaseContext(Context base) {
        installMultiDex(base);
        super.attachBaseContext(base);
    }

    protected void installMultiDex(Context base) {
        MultiDex.install(base);
    }

    /**
     * @deprecated This is not guaranteed to be initialised
     */
    @Deprecated
    public static QabelBoxApplication getInstance() {
        return mInstance;
    }

    public ApplicationComponent getApplicationComponent() {
        return ApplicationComponent;
    }

    private ApplicationComponent ApplicationComponent;

    public static ApplicationComponent getApplicationComponent(Context context) {
        return ((QabelBoxApplication) context.getApplicationContext()).getApplicationComponent();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        this.ApplicationComponent = initialiseInjector();
        mInstance = this;
        initService();
    }

    protected ApplicationComponent initialiseInjector() {
        return DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
    }

    void initService() {
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
                if (service instanceof LocalQabelService.LocalBinder) {
                    LocalQabelService.LocalBinder binder = (LocalQabelService.LocalBinder) service;
                    mService = binder.getService();
                    Log.d(TAG, "Service bound");
                } else {
                    Log.w(TAG, "Could not bind service");
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }
        };
    }

    /**
     * @deprecated Activities should get their own service
     */
    @Deprecated
    public LocalQabelService getService() {
        return mService;
    }

    /**
     * This application class create the service in onCreate.
     * if the application starts with mainActivity and bypass splash (e.g. share from outside, open app with file browser by mime type)
     * it can be that the activity get service early than the application class. because the service creation is asynchron
     * in same cases the app would crashed if acitivty have service and application not yet.
     * <p>
     * solution is to store service if early created from activity before from application.
     *
     * @param service
     */
    public void serviceCreatedOutside(LocalQabelService service) {
        if (mService == null) {
            Log.d(TAG, "service createdOutside");
            mService = service;
        }
    }
}
