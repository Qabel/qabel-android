package de.qabel.qabelbox.communication.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ConnectivityManager {

    private Context context;

    private android.net.ConnectivityManager connectivityManager;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (listener != null) {
                if (isConnected()) {
                    listener.handleConnectionEtablished();
                } else {
                    listener.handleConnectionLost();
                }
            }
        }
    };

    protected ConnectivityListener listener;

    public ConnectivityManager(Context context) {
        this.context = context;
        this.connectivityManager = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        context.registerReceiver(broadcastReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void onDestroy() {
        if(this.listener != null){
            this.listener.onDestroy();
        }
        context.unregisterReceiver(broadcastReceiver);
    }

    public boolean isConnected() {
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public boolean isWifi() {
        return connectivityManager.getActiveNetworkInfo().getType() == android.net.ConnectivityManager.TYPE_WIFI;
    }

    public void setListener(ConnectivityListener listener) {
        this.listener = listener;
    }

    public interface ConnectivityListener {

        void handleConnectionLost();

        void handleConnectionEtablished();

        void onDestroy();
    }
}
