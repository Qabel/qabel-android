package de.qabel.qabelbox.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import de.qabel.qabelbox.adapter.QabelSyncAdapter;

public class QabelSyncService extends Service {

    private static QabelSyncAdapter qabelSyncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object syncAdapterLock = new Object();


    @Override
    public void onCreate() {
        synchronized (syncAdapterLock) {
            if (qabelSyncAdapter == null) {
                qabelSyncAdapter = new
                        QabelSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return qabelSyncAdapter.getSyncAdapterBinder();
    }
}
