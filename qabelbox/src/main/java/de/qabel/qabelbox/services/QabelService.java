package de.qabel.qabelbox.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import de.qabel.ServiceConstants;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.qabelbox.R;

/**
 * QabelService hosts the DropActor and is responsible for sending and receiving
 * DropMessages. DropMessages are send and received from other applications via a Messenger.
 * Clients can register to receive certain DropMessages by sending a
 * {@link ServiceConstants.MSG_REGISTER_ON_TYPE} Message with the replyTo field pointing to an
 * incoming Messenger of the client.
 *
 * The service is started when a client binds to the service and stopped when the last client
 * unbinds.
 */
public class QabelService extends Service {

    private static final int SERVICE_NOTIFICATION_ID = 1;
    private static final String LOG_TAG_QABEL_SERVICE = "Qabel-Service";
    public static final String TAG = "QabelService";

    private final IncomingHandlerThread incomingHandlerThread = new IncomingHandlerThread();
    private final HashMap<String, Messenger> clientMessenger = new HashMap<>();

    private NotificationManager mNotificationManager;
    private LocalQabelService mService;

    class IncomingHandlerThread extends Thread {

        private Messenger mMessenger;

        public Messenger getmMessenger() {
            return mMessenger;
        }

        @Override
        public void run() {
            Looper.prepare();

            mMessenger = new Messenger(new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case ServiceConstants.MSG_REGISTER_ON_TYPE:
                            String dropMessageType = msg.getData().getString(ServiceConstants.DROP_MESSAGE_TYPE);
                            if (dropMessageType != null) {
                                clientMessenger.put(dropMessageType, msg.replyTo);
                            }
                            break;
                        case ServiceConstants.MSG_DROP_MESSAGE:
                            String dropPayloadType = msg.getData().getString(ServiceConstants.DROP_PAYLOAD_TYPE);
                            String dropPayload = msg.getData().getString(ServiceConstants.DROP_PAYLOAD);
                            String dropRecipientId = msg.getData().getString(ServiceConstants.DROP_RECIPIENT_ID);
                            String dropSenderId = msg.getData().getString(ServiceConstants.DROP_SENDER_ID);


                            Identity sender = mService.getIdentities().getByKeyIdentifier(dropSenderId);
                            Contact recipient =mService.getContacts().getByKeyIdentifier(dropRecipientId);

                            if (sender != null && recipient != null) {
                                DropMessage dropMessage = new DropMessage(sender, dropPayload, dropPayloadType);
                                Log.i(LOG_TAG_QABEL_SERVICE, "Sending received DropMessage");
                                try {
                                    mService.sendDropMessage(dropMessage, recipient, new LocalQabelService.OnSendDropMessageResult() {
                                        @Override
                                        public void onSendDropResult(Map<DropURL, Boolean> deliveryStatus) {
                                            //TODO: Ignored for now

                                        }
                                    });
                                } catch (QblDropPayloadSizeException e) {
                                    //TODO: Ignored for now
                                    e.printStackTrace();
                                }
                            }
                            break;
                        default:
                            super.handleMessage(msg);
                    }
                }
            });
            Looper.loop();
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        incomingHandlerThread.start();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        setNotification("Qabel Service starting");

		initServiceResources();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG_QABEL_SERVICE, "Client bound");
        return incomingHandlerThread.getmMessenger().getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotificationManager.cancel(SERVICE_NOTIFICATION_ID);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(LOG_TAG_QABEL_SERVICE, "All clients unbound. Stopping service!");
        stopSelf();
        return super.onUnbind(intent);
    }

    /**
     * Initialize DropActor and Module manager after connecting to the {@link LocalQabelService}
     */
    private void initServiceResources() {
        Log.i(LOG_TAG_QABEL_SERVICE, "Init resources");
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

        setNotification("Qabel Service running");
    }

    private void setNotification(String text) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification_template_icon_bg)
                        .setContentTitle("Qabel Service")
                        .setContentText(text)
                        .setSmallIcon(R.drawable.qabel_logo)
                        .setColor(getResources().getColor(R.color.colorPrimary));

        mNotificationManager.notify(SERVICE_NOTIFICATION_ID, mBuilder.build());
    }

    // TODO: Needs to be called from LocalQabelService via EventBus
    public void onMessageReceived(DropMessage dropMessage) {
        Messenger msgr = clientMessenger.get(dropMessage.getDropPayloadType());
        if (msgr != null) {
            Log.i(LOG_TAG_QABEL_SERVICE, "Delivering received DropMessage");
            Message msg = Message.obtain(null, ServiceConstants.MSG_DROP_MESSAGE);
            Bundle bundle = new Bundle();
            bundle.putString(ServiceConstants.DROP_SENDER_ID, dropMessage.getSenderKeyId());
            bundle.putString(ServiceConstants.DROP_PAYLOAD_TYPE, dropMessage.getDropPayloadType());
            bundle.putString(ServiceConstants.DROP_PAYLOAD, dropMessage.getDropPayload());
            msg.setData(bundle);
            try {
                msgr.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending message to drop message recipient", e);
            }
        }
    }
}
