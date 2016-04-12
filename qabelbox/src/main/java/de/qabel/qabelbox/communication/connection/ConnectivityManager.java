package de.qabel.qabelbox.communication.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;

public class ConnectivityManager {

    private Context context;

    private android.net.ConnectivityManager connectivityManager;
    private NetworkInfo activeNetwork;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (listener != null) {
                if (isConnected()) {
                    listener.handleConnectionEtablished();
                } else {
                    listener.handleConnectionLost();
                }
            }
        }
    };

    private ConnectivityListener listener;

    public ConnectivityManager(Context context) {
        this.context = context;
        this.connectivityManager = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        activeNetwork = connectivityManager.getActiveNetworkInfo();

        context.registerReceiver(broadcastReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void onDestroy() {
        context.unregisterReceiver(broadcastReceiver);
    }

    public boolean isConnected() {
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public boolean isWifi() {
        return activeNetwork != null && activeNetwork.isConnected() && activeNetwork.getType() == android.net.ConnectivityManager.TYPE_WIFI;
    }

    public void setListener(ConnectivityListener listener) {
        this.listener = listener;
    }

    public interface ConnectivityListener {

        void handleConnectionLost();

        void handleConnectionEtablished();

    }
}
