package de.qabel.qabelbox.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.ArrayList;
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
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.fragments.AddContactFragment;
import de.qabel.qabelbox.fragments.AddIdentityFragment;
import de.qabel.qabelbox.fragments.ContactFragment;
import de.qabel.qabelbox.fragments.NewDatabasePasswordFragment;
import de.qabel.qabelbox.fragments.OpenDatabaseFragment;
import de.qabel.qabelbox.fragments.SelectIdentityFragment;
import de.qabel.qabelbox.storage.BoxExternal;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxVolume;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.fragments.FilesFragment;
import de.qabel.qabelbox.fragments.NewFolderFragment;
import de.qabel.qabelbox.fragments.SelectUploadFolderFragment;
import de.qabel.qabelbox.providers.BoxProvider;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    SelectUploadFolderFragment.OnSelectedUploadFolderListener,
                        NewFolderFragment.OnFragmentInteractionListener,
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

    private static final String TAG = "BoxMainActivity";
    private static final int REQUEST_CODE_OPEN = 11;
    private static final int REQUEST_CODE_UPLOAD_FILE = 12;
    public static final String HARDCODED_ROOT = BoxProvider.PUB_KEY
            + BoxProvider.DOCID_SEPARATOR + BoxProvider.BUCKET + BoxProvider.DOCID_SEPARATOR
            + BoxProvider.PREFIX + BoxProvider.DOCID_SEPARATOR + BoxProvider.PATH_SEP;
    private static final int REQUEST_CODE_DELETE_FILE = 13;
    private Identity activeIdentity;
    private ResourceActor resourceActor;
    private ProviderActor providerActor;
    private Thread providerActorThread;
    private HashMap<Identity, Contacts> contacts;
    private Identities identities;
    private BoxVolume boxVolume;
    private BoxProvider provider;
    private FloatingActionButton fab;
    private TextView textViewSelectedIdentity;
    private Activity self;
    private View appBarMain;

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
            resourceActor = QabelBoxApplication.getResourceActor();

            providerActor = new ProviderActor();
            providerActorThread = new Thread(providerActor, "ProviderActorThread");
            providerActorThread.start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Uri uri;
        if (requestCode == REQUEST_CODE_OPEN && resultCode == Activity.RESULT_OK && data != null) {
            uri = data.getData();
            Log.i(TAG, "Uri: " + uri.toString());
            Intent viewIntent = new Intent();
            String type = URLConnection.guessContentTypeFromName(uri.toString());
            Log.i(TAG, "Mime type: " + type);
            viewIntent.setDataAndType(uri, type);
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(viewIntent, "Open with"));
            return;
        }
        if (requestCode == REQUEST_CODE_UPLOAD_FILE && resultCode == Activity.RESULT_OK && data != null) {
            uri = data.getData();
            uploadUri(uri);
            return;
        }
        if (requestCode == REQUEST_CODE_DELETE_FILE && resultCode == Activity.RESULT_OK && data != null) {
            uri = data.getData();
            Log.i(TAG, "Deleting file: " + uri.toString());
            new AsyncTask<Uri, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Uri... params) {
                    return DocumentsContract.deleteDocument(getContentResolver(), params[0]);
                }
            }.execute(uri);
            return;
        }
    }

    private boolean uploadUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "No valid url for uploading" + uri);
            return true;
        }
        cursor.moveToFirst();
        String displayName = cursor.getString(
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        Log.i(TAG, "Displayname: " + displayName);
        Uri uploadUri = DocumentsContract.buildDocumentUri(
                BoxProvider.AUTHORITY, HARDCODED_ROOT + displayName);
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uploadUri, "w");
            if (outputStream == null) {
                return false;
            }
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return false;
            }
            IOUtils.copy(inputStream, outputStream);
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error opening output stream for upload", e);
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        self = this;
        provider = ((QabelBoxApplication) getApplication()).getProvider();
        Log.i(TAG, "Provider: " + provider);
        boxVolume = provider.getVolumeForRoot(null, null, null);

        contacts = new HashMap<>();
        identities = new Identities();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
        appBarMain = findViewById(R.id.app_bap_main);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Map QR-Code indent to alias textview in nav_header_main
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

        // Check if activity is started with ACTION_SEND or ACTION_SEND_MULTIPLE
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        Log.i(TAG, "Intent action: " + action);

        // Checks if a fragment should be launched
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
            case Intent.ACTION_SEND:
                if (type != null) {
                    Log.i(TAG, "Action send in main activity");
                    Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (imageUri != null) {
                        browseTo(null, imageUri);
                    }
                }
                break;
            case Intent.ACTION_SEND_MULTIPLE:
                if (type != null) {
                    ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    //TODO: Implement
                }
                break;
            default:
                browseTo(null, null);
                break;
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(new ResourceReadyReceiver(),
                new IntentFilter(QabelBoxApplication.RESOURCES_INITIALIZED));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        } else {
            getFragmentManager().popBackStack();
        }
        if (getFragmentManager().getBackStackEntryCount() == 1) {
            super.onBackPressed();
        }
    }

    private void browseTo(final BoxFolder navigateTo, final Uri uploadURI) {
        new AsyncTask<Void, Void, Void>() {
            FilesFragment filesFragment;
            FilesAdapter filesAdapter;
            BoxNavigation boxNavigation;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (uploadURI != null) {
                    filesFragment = new SelectUploadFolderFragment();
                    ((SelectUploadFolderFragment)filesFragment).setUri(uploadURI);
                } else {
                    filesFragment = new FilesFragment();
                }
                filesFragment.setLoadingSpinner(true);
                filesAdapter = new FilesAdapter(new ArrayList<BoxObject>());
                filesFragment.setAdapter(filesAdapter);
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, filesFragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    boxNavigation = boxVolume.navigate();
                    if (navigateTo != null) {
                        boxNavigation.navigate(navigateTo);
                    }
                    filesFragment.setBoxNavigation(boxNavigation);
                    for (BoxFolder boxFolder : boxNavigation.listFolders()){
                        Log.d(TAG, "Adding folder: " + boxFolder.name);
                        filesAdapter.add(boxFolder);
                    }
                    for (BoxExternal boxExternal : boxNavigation.listExternals()){
                        Log.d("MainActivity", "Adding external: " + boxExternal.name);
                        filesAdapter.add(boxExternal);
                    }
                    if (uploadURI == null) {
                        for (BoxFile boxFile : boxNavigation.listFiles()) {
                            Log.d(TAG, "Adding file: " + boxFile.name);
                            filesAdapter.add(boxFile);
                        }
                    }
                    filesAdapter.sort();

                    filesAdapter.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(View view, int position) {
                            final BoxObject boxObject = filesAdapter.get(position);
                            if (boxObject instanceof BoxFolder) {
                                browseTo(((BoxFolder) boxObject), uploadURI);
                            } else if (boxObject instanceof BoxFile) {
                                // Open
                                String path = boxNavigation.getPath(boxObject);
                                String documentId = boxVolume.getDocumentId(path);
                                Uri uri = DocumentsContract.buildDocumentUri(
                                        BoxProvider.AUTHORITY, documentId);
                                Intent viewIntent = new Intent();
                                String type = URLConnection.guessContentTypeFromName(uri.toString());
                                Log.i(TAG, "Mime type: " + type);
                                viewIntent.setDataAndType(uri, type);
                                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(Intent.createChooser(viewIntent, "Open with"));
                            }
                        }
                    });
                } catch (QblStorageException e) {
                    Log.e(TAG, "browseTo failed", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                fab.setVisibility(View.VISIBLE);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final NewFolderFragment newFolderFragment = new NewFolderFragment();
                        new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                newFolderFragment.setBoxNavigation(boxNavigation);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                super.onPostExecute(aVoid);
                                getFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, newFolderFragment)
                                        .addToBackStack(null)
                                        .commit();
                            }
                        }.execute();
                    }
                });
                filesFragment.setLoadingSpinner(false);
                filesAdapter.notifyDataSetChanged();
            }
        }.execute();
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
        } else if (id == R.id.nav_browse) {
            browseTo(null, null);
        } else if (id == R.id.nav_open) {
            Intent intentOpen = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intentOpen.addCategory(Intent.CATEGORY_OPENABLE);
            intentOpen.setType("*/*");
            startActivityForResult(intentOpen, REQUEST_CODE_OPEN);
        } else if (id == R.id.nav_upload) {
            Intent intentOpen = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intentOpen.addCategory(Intent.CATEGORY_OPENABLE);
            intentOpen.setType("*/*");
            startActivityForResult(intentOpen, REQUEST_CODE_UPLOAD_FILE);
        } else if (id == R.id.nav_delete) {
            Intent intentOpen = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intentOpen.addCategory(Intent.CATEGORY_OPENABLE);
            intentOpen.setType("*/*");
            startActivityForResult(intentOpen, REQUEST_CODE_DELETE_FILE);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onFolderSelected(final Uri uri, final BoxNavigation boxNavigation) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
				Cursor returnCursor =
						getContentResolver().query(uri, null, null, null, null);
				int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				returnCursor.moveToFirst();
				String name = returnCursor.getString(nameIndex);
				returnCursor.close();

                try {
                    String path = boxNavigation.getPath();
                    String folderId = boxVolume.getDocumentId(path);
                    Uri uploadUri = DocumentsContract.buildDocumentUri(
                            BoxProvider.AUTHORITY, folderId + BoxProvider.PATH_SEP + name);

                    InputStream content = getContentResolver().openInputStream(uri);
                    OutputStream upload = getContentResolver().openOutputStream(uploadUri, "w");
                    if (upload == null || content == null) {
                        finish();
                        return null;
                    }
                    IOUtils.copy(content, upload);
                    content.close();
                    upload.close();
                } catch (IOException e) {
                    Log.e(TAG, "Upload failed", e);
                }
                return null;
            }
        }.execute();
        finish();
    }

    @Override
    public void onAbort() {

    }

    @Override
    public void onCreateFolder(final String name, final BoxNavigation boxNavigation) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    boxNavigation.createFolder(name);
                    boxNavigation.commit();
                } catch (QblStorageException e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                browseTo(null, null);
            }
        }.execute();
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
        ((QabelBoxApplication) getApplication()).init(newPassword);
        selectContactsFragment();
    }

    @Override
    public void onPasswordEntered(char[] password) {
        if (((QabelBoxApplication) getApplication()).init(password)) {
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
