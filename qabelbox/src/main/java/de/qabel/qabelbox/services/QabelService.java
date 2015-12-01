package de.qabel.qabelbox.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import de.qabel.ServiceConstants;
import de.qabel.ackack.MessageInfo;
import de.qabel.ackack.Responsible;
import de.qabel.ackack.event.EventActor;
import de.qabel.ackack.event.EventEmitter;
import de.qabel.ackack.event.EventListener;
import de.qabel.core.EventNameConstants;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.config.ResourceActor;
import de.qabel.core.drop.DropActor;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.module.ModuleManager;
import de.qabel.qabelbox.QabelBoxApplication;
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
    private static final long DEFAULT_DROP_POLL_INTERVAL = 5000L;

    private final IncomingHandlerThread incomingHandlerThread = new IncomingHandlerThread();
    private final HashMap<String, Messenger> clientMessenger = new HashMap<>();
    private final EventEmitter emitter = EventEmitter.getDefault();

    private DropActor dropActor;
    private ReceiverModule receiverModule;

    private ModuleManager moduleManager;
    private NotificationManager mNotificationManager;
    private ResourceActor resourceActor;
    private HashMap<String, Identity> identities;
    private HashMap<String, Contact> contacts;
    private Thread providerActorThread;

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
                    // Wait with message processing until resources are initialized
                    // TODO: Find better solution
                    while (receiverModule == null) {
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    switch (msg.what) {
                        case ServiceConstants.MSG_REGISTER_ON_TYPE:
                            String dropMessageType = msg.getData().getString(ServiceConstants.DROP_MESSAGE_TYPE);
                            if (dropMessageType != null) {
                                receiverModule.registerEvent(dropMessageType);
                                clientMessenger.put(dropMessageType, msg.replyTo);
                            }
                            break;
                        case ServiceConstants.MSG_DROP_MESSAGE:
                            String dropPayloadType = msg.getData().getString(ServiceConstants.DROP_PAYLOAD_TYPE);
                            String dropPayload = msg.getData().getString(ServiceConstants.DROP_PAYLOAD);
                            String dropRecipientId = msg.getData().getString(ServiceConstants.DROP_RECIPIENT_ID);
                            String dropSenderId = msg.getData().getString(ServiceConstants.DROP_SENDER_ID);

                            Identity sender = identities.get(dropSenderId);
                            Contact recipient = contacts.get(dropRecipientId);

                            if (sender != null && recipient != null) {
                                DropMessage dropMessage = new DropMessage(sender, dropPayload, dropPayloadType);
                                Log.i(LOG_TAG_QABEL_SERVICE, "Sending received DropMessage");
                                DropActor.send(EventEmitter.getDefault(), dropMessage, recipient);
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

    /**
     * Loads qabel resources from ResourceActor
     */
    class ProviderActor extends EventActor implements EventListener {
        public ProviderActor() {
            on(EventNameConstants.EVENT_CONTACT_ADDED, this);
            on(EventNameConstants.EVENT_IDENTITY_ADDED, this);

            resourceActor.retrieveContacts(this, new Responsible() {
                @Override
                public void onResponse(Serializable... data) {
                    ArrayList<Contact> receivedContacts = new ArrayList<>(Arrays.asList((Contact[]) data));
                    for (Contact c : receivedContacts) {
                        contacts.put(c.getKeyIdentifier(), c);
                    }
                }
            });

            resourceActor.retrieveIdentities(this, new Responsible() {
                @Override
                public void onResponse(Serializable... data) {
                    for (Identity identity : (Identity[]) data) {
                      identities.put(identity.getKeyIdentifier(), identity);
                    }
                }
            });
        }

        @Override
        public void onEvent(String event, MessageInfo info, Object... data) {
            switch (event) {
                case EventNameConstants.EVENT_CONTACT_ADDED:
                    if (data[0] instanceof Contact) {
                        Contact c = (Contact) data[0];
                        contacts.put(c.getKeyIdentifier(), c);
                    }
                    break;
                case EventNameConstants.EVENT_IDENTITY_ADDED:
                    if (data[0] instanceof Identity) {
                        Identity i = (Identity) data[0];
                        identities.put(i.getKeyIdentifier(), i);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Starts initialization of QabelService resources when global resources are ready
     */
    class ResourceReadyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            initServiceResources();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        incomingHandlerThread.start();

        contacts = new HashMap<>();
        identities = new HashMap<>();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        setNotification("Qabel Service waiting for database unlock");

        /**
         *  If global resources are not ready, register for RESOURCES_INITIALIZED event. Else init
         *  local resources
         */
        if (QabelBoxApplication.isResourceActorInitialized()) {
            initServiceResources();
        } else {
            LocalBroadcastManager.getInstance(this).registerReceiver(new ResourceReadyReceiver(),
                    new IntentFilter(QabelBoxApplication.RESOURCES_INITIALIZED));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG_QABEL_SERVICE, "Client bound");
        return incomingHandlerThread.getmMessenger().getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (dropActor != null) {
            dropActor.stop();
        }
        if (moduleManager != null) {
            moduleManager.shutdown();
        }

        mNotificationManager.cancel(SERVICE_NOTIFICATION_ID);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(LOG_TAG_QABEL_SERVICE, "All clients unbound. Stopping service!");
        stopSelf();
        return super.onUnbind(intent);
    }

    /**
     * Initialize DropActor and Module manager. Requires global ResourceActor to be ready.
     * Thus started when receiving IQabelServiceInternal.RESOURCES_INITIALIZED
     */
    private void initServiceResources() {
        Log.i(LOG_TAG_QABEL_SERVICE, "Init resources");
        resourceActor = QabelBoxApplication.getResourceActor();
        providerActorThread = new Thread(new ProviderActor());
        providerActorThread.start();
        dropActor = new DropActor(QabelBoxApplication.getResourceActor(), emitter);
        dropActor.setInterval(DEFAULT_DROP_POLL_INTERVAL);
        Thread dropActorThread = new Thread(dropActor, "DropActorThread");
        dropActorThread.start();
        moduleManager = new ModuleManager(emitter, QabelBoxApplication.getResourceActor());
        try {
            receiverModule = moduleManager.startModule(ReceiverModule.class);
            receiverModule.setMessageReceivedInterface(new ReceiverModule.MessageReceivedInterface() {
                @Override
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
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

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
}
