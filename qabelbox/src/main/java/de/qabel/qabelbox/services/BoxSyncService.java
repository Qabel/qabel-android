package de.qabel.qabelbox.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import de.qabel.qabelbox.adapter.BoxSyncAdapter;

public class BoxSyncService extends Service {

    private static BoxSyncAdapter boxSyncAdapter = null;
    // Object to use as a thread-safe lock (from official Android documentation!!)
    private static final Object syncAdapterLock = new Object();


    @Override
    public void onCreate() {
        synchronized (syncAdapterLock) {
            if (boxSyncAdapter == null) {
                boxSyncAdapter = new
                    BoxSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return boxSyncAdapter.getSyncAdapterBinder();
    }
}
