package de.qabel.qabellauncher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;

import java.io.Serializable;
import java.util.HashMap;

import de.qabel.ackack.MessageInfo;
import de.qabel.ackack.Responsible;
import de.qabel.ackack.event.EventActor;
import de.qabel.ackack.event.EventListener;
import de.qabel.core.EventNameConstants;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.config.ResourceActor;
import de.qabel.qabellauncher.fragments.AddContactFragment;
import de.qabel.qabellauncher.fragments.AddIdentityFragment;
import de.qabel.qabellauncher.fragments.ContactFragment;
import de.qabel.qabellauncher.fragments.NewDatabasePasswordFragment;
import de.qabel.qabellauncher.fragments.OpenDatabaseFragment;
import de.qabel.qabellauncher.fragments.SelectIdentityFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    ContactFragment.ContactListListener,
                        SelectIdentityFragment.SelectIdentityListener,
                            AddIdentityFragment.AddIdentityListener,
                                NewDatabasePasswordFragment.NewDatabasePasswordListener,
                                    OpenDatabaseFragment.OpenDatabaseFragmentListener,
                                        AddContactFragment.AddContactListener {

    public static final String ACTION_ENTER_DB_PASSWORD = "EnterDatabasePassword";
    public static final String ACTION_ENTER_NEW_DB_PASSWORD = "EnterNewDatabasePassword";

    private static final String TAG_OPEN_DATABASE_FRAGMENT = "OPEN_DATABASE_FRAGMENT";
    private static final String TAG_NEW_DATABASE_PASSWORD_FRAGMENT = "NEW_DATABASE_PASSWORD_FRAGMENT";

    private HashMap<Identity, Contacts> contacts;
    private Identities identities;
    private Identity activeIdentity;
    private ResourceActor resourceActor;
    private ProviderActor providerActor;
    private Thread providerActorThread;
    private TextView textViewSelectedIdentity;
    private NavigationView navigationView;
    private Activity self;
    private View appBarMain;
    private FloatingActionButton fab;

    class ProviderActor extends EventActor implements EventListener {
        public ProviderActor() {
            on(EventNameConstants.EVENT_CONTACT_ADDED, this);
            on(EventNameConstants.EVENT_IDENTITY_ADDED, this);

            resourceActor.retrieveIdentities(this, new Responsible() {
                @Override
                public void onResponse(Serializable... data) {
                    for (Identity identity : (Identity[]) data) {
                        if (contacts.get(identity) == null) {
                            contacts.put(identity, new Contacts());
                        }
                        identities.put(identity);
                    }
                }
            });

            resourceActor.retrieveContacts(this, new Responsible() {
                @Override
                public void onResponse(Serializable... data) {
                    for (Contact contact : (Contact[]) data) {
                        Contacts identityContacts = contacts.get(contact.getContactOwner());
                        if (identityContacts == null) {
                            identityContacts = new Contacts();
                        }
                        identityContacts.put(contact);
                        contacts.put(contact.getContactOwner(), identityContacts);
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
                        Contacts identityContacts = contacts.get(c.getContactOwner());
                        if (identityContacts == null) {
                            identityContacts = new Contacts();
                        }
                        identityContacts.put(c);
                        contacts.put(c.getContactOwner(), identityContacts);
                    }
                    break;
                case EventNameConstants.EVENT_IDENTITY_ADDED:
                    if (data[0] instanceof Identity) {
                        Identity i = (Identity) data[0];
                        if (contacts.get(i) == null) {
                            contacts.put(i, new Contacts());
                        }
                        identities.put(i);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Starts initialization when global resources are ready
     */
    class ResourceReadyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            resourceActor = QabelLauncherApplication.getResourceActor();

            providerActor = new ProviderActor();
            providerActorThread = new Thread(providerActor, "ProviderActorThread");
            providerActorThread.start();
        }
    }

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

        fab = (FloatingActionButton) findViewById(R.id.fab_add);
        fab.setVisibility(View.INVISIBLE);

        appBarMain = findViewById(R.id.app_bap_main);

        self = this;

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        textViewSelectedIdentity = (TextView) navigationView.findViewById(R.id.textViewSelectedIdentity);
        textViewSelectedIdentity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activeIdentity != null) {
                    IntentIntegrator intentIntegrator = new IntentIntegrator(self);
                    intentIntegrator.shareText("QABELCONTACT\n"
                            + activeIdentity.getAlias() + "\n"
                            + activeIdentity.getDropUrls().toArray()[0].toString() + "\n"
                            + activeIdentity.getKeyIdentifier());
                }
            }
        });

        contacts = new HashMap<>();
        identities = new Identities();

        // Checks if a fragment should be launched
        Intent intent = getIntent();
        switch (intent.getAction()) {
            case ACTION_ENTER_DB_PASSWORD:
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new OpenDatabaseFragment(), TAG_OPEN_DATABASE_FRAGMENT)
                        .commit();
                break;
            case ACTION_ENTER_NEW_DB_PASSWORD:
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new NewDatabasePasswordFragment(), TAG_NEW_DATABASE_PASSWORD_FRAGMENT)
                        .commit();
                break;
            default:
                break;
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(new ResourceReadyReceiver(),
                new IntentFilter(QabelLauncherApplication.RESOURCES_INITIALIZED));
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_contacts) {
            selectContactsFragment();
        } else if (id == R.id.nav_identities) {
            selectIdentityFragment();
        } else if (id == R.id.nav_files) {
            Snackbar.make(appBarMain, "Not implemented yet!", Snackbar.LENGTH_LONG)
                    .show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void selectContactsFragment() {
        if (activeIdentity == null) {
            loadSelectIdentityFragment();
        } else {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startAddContact(activeIdentity);
                }
            });
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, ContactFragment.newInstance(contacts.get(activeIdentity), activeIdentity))
                    .commit();
        }
    }

    private void selectIdentityFragment() {
        if (identities == null || identities.getIdentities() == null ||
                identities.getIdentities().isEmpty()) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddIdentityFragment())
                    .commit();
        } else {
            loadSelectIdentityFragment();
        }
    }

    private void loadSelectIdentityFragment() {
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAddIdentity();
            }
        });
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, SelectIdentityFragment.newInstance(identities))
                .commit();
    }

    @Override
    public void startAddContact(Identity identity) {
        fab.setVisibility(View.INVISIBLE);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, AddContactFragment.newInstance(identity))
                .commit();
    }

    @Override
    public void selectIdentity(Identity identity) {
        activeIdentity = identity;
        selectContactsFragment();

        textViewSelectedIdentity.setText(identity.getAlias());
    }

    @Override
    public void startAddIdentity() {
        fab.setVisibility(View.INVISIBLE);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AddIdentityFragment())
                .commit();
    }

    @Override
    public void addIdentity(Identity identity) {
        resourceActor.writeIdentities(identity);
        activeIdentity = identity;

        contacts.put(identity, new Contacts());

        selectIdentityFragment();

        Snackbar.make(appBarMain, "Added identity: " + identity.getAlias(), Snackbar.LENGTH_LONG)
                .show();
    }

    @Override
    public void onNewPasswordEntered(char[] newPassword) {
        ((QabelLauncherApplication) getApplication()).init(newPassword);
        selectContactsFragment();
    }

    @Override
    public void onPasswordEntered(char[] password) {
        if (((QabelLauncherApplication) getApplication()).init(password)) {
            selectContactsFragment();
        } else {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new OpenDatabaseFragment(), TAG_OPEN_DATABASE_FRAGMENT)
                    .commit();
        }
    }

    @Override
    public void addContact(Contact contact) {
        resourceActor.writeContacts(contact);
        selectIdentityFragment();

        Snackbar.make(appBarMain, "Added contact: " + contact.getAlias(), Snackbar.LENGTH_LONG)
                .show();
    }
}
