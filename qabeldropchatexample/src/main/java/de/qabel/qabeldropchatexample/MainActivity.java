package de.qabel.qabeldropchatexample;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import de.qabel.IContact;
import de.qabel.IIdentity;
import de.qabel.ServiceConstants;
import de.qabel.QabelContentProviderConstants;
import de.qabel.qabeldropchatexample.fragments.ContactListFragment;
import de.qabel.qabeldropchatexample.fragments.HelloWorldFragment;
import de.qabel.qabeldropchatexample.module.hello_world.HelloWorldObject;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
            ContactListFragment.OnContactSelectedListener,
                HelloWorldFragment.OnSendMessageInterface {

    private final Gson gson = new Gson();
    private final Messenger incomingMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ServiceConstants.MSG_DROP_MESSAGE:
                    handleIncomingDropMessage(msg);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    });

    private Messenger outgoingMessenger;
    private boolean mBoundToQabelService;
    private boolean contactsReceived;
    private boolean identitiesReceived;

    private HelloWorldFragment activeFragment;

    private IContact activeContact;
    private ArrayList<IIdentity> identities;
    private HashMap<String, ArrayList<IContact>> identityContacts;
    // Workaround to show all contacts with the belonging identity in one list
    private ArrayList<IContact> allContacts;
    private HashMap<IContact, TreeMap<Long, String>> messages;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            outgoingMessenger = new Messenger(service);
            mBoundToQabelService = true;

            Message msg = Message.obtain(null, ServiceConstants.MSG_REGISTER_ON_TYPE);
            msg.replyTo = incomingMessenger;
            Bundle bundle = new Bundle();
            bundle.putString(ServiceConstants.DROP_MESSAGE_TYPE, HelloWorldFragment.HELLO_WORLD_TYPE);
            msg.setData(bundle);
            try {
                outgoingMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "Connected to Qabel service", Toast.LENGTH_LONG).show();
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
            outgoingMessenger = null;
            mBoundToQabelService = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        messages = new HashMap<>();
        identities = new ArrayList<>();
        identityContacts = new HashMap<>();
        allContacts = new ArrayList<>();

        loadResources();

        Intent intent = new Intent(ServiceConstants.SERVICE_CLASS_NAME);
        intent.setPackage(ServiceConstants.SERVICE_PACKAGE_NAME);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    private void loadResources() {
        // Workaround since resources are loaded in onNavigationDrawerItemSelected on launch
        // before resources are initialized
        if (identities == null || identityContacts == null || allContacts == null) {
            return;
        }
        if (!identitiesReceived) {
            loadIdentities();
        }
        if (!contactsReceived) {
            loadContacts();
        }
    }

    private void loadContacts() {
        Uri path = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(QabelContentProviderConstants.CONTENT_AUTHORITY)
                .appendPath(QabelContentProviderConstants.CONTENT_CONTACTS)
                .build();

        Cursor cursor = getContentResolver().query(
                path,
                null,
                null,
                null,
                null);

        if (cursor == null) {
            return;
        }
        contactsReceived = true;

        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            String identityId = cursor.getString(1);
            String id = cursor.getString(2);

            ArrayList<IContact> contacts = identityContacts.get(identityId);
            if (contacts == null) {
                contacts = new ArrayList<>();
            }
            for (IIdentity i : identities) {
                if (i.getId().equals(identityId)) {
                    IContact contact = new IContact(name, identityId, id, i.getAlias());
                    contacts.add(contact);
                    identityContacts.put(identityId, contacts);
                    // Workaround to get global contact list
                    allContacts.add(contact);
                    break;
                }
            }
        }
        cursor.close();
    }

    private void loadIdentities() {
        Uri path = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(QabelContentProviderConstants.CONTENT_AUTHORITY)
                .appendPath(QabelContentProviderConstants.CONTENT_IDENTITIES)
                .build();

        Cursor cursor = getContentResolver().query(
                path,
                null,
                null,
                null,
                null);

        if (cursor == null) {
            return;
        }
        identitiesReceived = true;

        while (cursor.moveToNext()) {
            identities.add(new IIdentity(cursor.getString(0), cursor.getString(1)));
        }
        cursor.close();
    }


    @Override
    public void onContactSelected(IContact contact) {
        activeFragment = HelloWorldFragment.newInstance(contact);
        activeContact = contact;

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, activeFragment)
                .commit();

        setMessageText(contact);
    }

    @Override
    public void onSendDropMessage(IContact contact, String message, String type) {
        HelloWorldObject data = new HelloWorldObject();
        data.setStr(message);
        data.setTimestamp(System.currentTimeMillis());

        if (mBoundToQabelService) {
            Message msg = Message.obtain(null, ServiceConstants.MSG_DROP_MESSAGE);
            Bundle bundle = new Bundle();
            bundle.putString(ServiceConstants.DROP_PAYLOAD, gson.toJson(data));
            bundle.putString(ServiceConstants.DROP_PAYLOAD_TYPE, type);
            bundle.putString(ServiceConstants.DROP_SENDER_ID, contact.getContactOwnerId());
            bundle.putString(ServiceConstants.DROP_RECIPIENT_ID, contact.getEcPublicKey());
            msg.setData(bundle);
            try {
                outgoingMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        addAndSetMessageText(contact, "> " + message, data.getTimestamp());
    }

    private void handleIncomingDropMessage(Message msg) {
        String dropPayloadType = msg.getData().getString(ServiceConstants.DROP_PAYLOAD_TYPE);
        String dropPayload = msg.getData().getString(ServiceConstants.DROP_PAYLOAD);
        String senderId = msg.getData().getString(ServiceConstants.DROP_SENDER_ID);
        if (dropPayloadType == null || dropPayload == null || senderId == null) {
            return;
        }

        switch (dropPayloadType) {
            case HelloWorldFragment.HELLO_WORLD_TYPE:
                try {
                    HelloWorldObject payload = gson.fromJson(dropPayload, HelloWorldObject.class);
                    for (ArrayList<IContact> contacts : identityContacts.values()) {
                        for (IContact c : contacts) {
                            if (c.getEcPublicKey().equals(senderId)) {

                                addAndSetMessageText(c, c.getContactOwnerId() + "> " +
                                                payload.getStr(),
                                        payload.getTimestamp());
                                break;
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    Log.e("MainActivity", "Cannot read DropMessage!");
                }
                break;
            default:
                Log.wtf("MainActivity", "Unknown DropMessage type received!");
        }
    }

    private void addAndSetMessageText(IContact contact, String message, Long timestamp) {
        TreeMap<Long, String> contactMessages = messages.get(contact);
        if (contactMessages == null) {
            contactMessages = new TreeMap<>();
        }
        contactMessages.put(timestamp, message);
        messages.put(contact, contactMessages);

        setMessageText(contact);
    }

    private void setMessageText(IContact contact) {
        TreeMap<Long, String> contactMessages = messages.get(contact);
        String messageLog = "";
        if (contactMessages != null) {
            for (String s : contactMessages.values()) {
                messageLog += s + "\n";
            }
        }
        if (activeFragment != null && contact.equals(activeContact)) {
            activeFragment.setMessageText(messageLog);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        loadResources();

        if (id == R.id.nav_identities) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, ContactListFragment.newInstance(allContacts))
                    .commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}