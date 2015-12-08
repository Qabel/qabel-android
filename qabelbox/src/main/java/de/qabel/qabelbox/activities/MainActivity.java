package de.qabel.qabelbox.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.bottomsheet.BottomSheet;
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
                                    AddIdentityFragment.AddIdentityListener,
                                        NewDatabasePasswordFragment.NewDatabasePasswordListener,
                                            OpenDatabaseFragment.OpenDatabaseFragmentListener,
                                                AddContactFragment.AddContactListener,
                                                    FilesFragment.FilesListListener{

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
    private static final int REQUEST_CODE_CHOOSE_EXPORT = 14;
    private static final String FALLBACK_MIMETYPE = "application/octet-stream";
    private static final int NAV_GROUP_IDENTITIES = 1;
    private static final int NAV_GROUP_IDENTITY_ACTIONS = 2;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
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
    private FilesFragment filesFragment;
    private Toolbar toolbar;
    private ImageView imageViewExpandIdentity;
    private boolean identityMenuExpanded;

    // Used to save the document uri that should exported while waiting for the result
    // of the create document intent.
    private Uri exportUri;

    class ProviderActor extends EventActor implements EventListener {
        public ProviderActor() {
            on(EventNameConstants.EVENT_CONTACT_ADDED, this);
            on(EventNameConstants.EVENT_IDENTITY_ADDED, this);

            resourceActor.retrieveIdentities(this, new Responsible() {
                @Override
                public void onResponse(Serializable... data) {
                    final Identity[] identitiesArray = (Identity[]) data;
                    for (Identity identity : identitiesArray) {
                        if (contacts.get(identity) == null) {
                            contacts.put(identity, new Contacts());
                        }
                        identities.put(identity);
                    }

                    String lastID = QabelBoxApplication.getLastActiveIdentityID();
                    if (!lastID.equals("")) {
                        for (Identity identity : identities.getIdentities()) {
                            if (identity.getPersistenceID().equals(lastID)) {
                                activeIdentity = identity;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        textViewSelectedIdentity.setText(activeIdentity.getAlias());
                                    }
                                });
                                break;
                            }
                        }
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
        if (requestCode == REQUEST_CODE_CHOOSE_EXPORT && resultCode == Activity.RESULT_OK && data != null) {
            uri = data.getData();
            Log.i(TAG, "Export uri chosen: "+ uri.toString());
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(exportUri);
                        OutputStream outputStream = getContentResolver().openOutputStream(uri);
                        if (inputStream == null || outputStream == null) {
                            Log.e(TAG, "could not open streams");
                            return null;
                        }
                        IOUtils.copy(inputStream, outputStream);
                        inputStream.close();
                        outputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to export file", e);
                    }
                    return null;
                }
            }.execute();
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
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        self = this;
        provider = ((QabelBoxApplication) getApplication()).getProvider();
        Log.i(TAG, "Provider: " + provider);
        boxVolume = provider.getVolumeForRoot(null, null, null);

        filesFragment = FilesFragment.newInstance(boxVolume);
        filesFragment.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    final BoxObject boxObject = filesFragment.getFilesAdapter().get(position);
                    if (boxObject instanceof BoxFolder) {
                        filesFragment.browseTo(((BoxFolder) boxObject));
                    } else if (boxObject instanceof BoxFile) {
                        // Open
                        String path = filesFragment.getBoxNavigation().getPath(boxObject);
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
            @Override
            public void onItemLockClick(View view, final int position) {
                final BoxObject boxObject = filesFragment.getFilesAdapter().get(position);
                new BottomSheet.Builder(self).title(boxObject.name).sheet(R.menu.files_bottom_sheet)
                        .listener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case R.id.share:
                                        Toast.makeText(self, R.string.not_implemented,
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                    case R.id.delete:
                                        delete(boxObject);
                                        break;
                                    case R.id.export:
                                        // Export handled in the MainActivity
                                        if (boxObject instanceof BoxFolder) {
                                            Toast.makeText(self, R.string.folder_export_not_implemented,
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            onExport(filesFragment.getBoxNavigation(), boxObject);
                                        }
                                        break;
                                }
                            }
                        }).show();

            }
            });

        contacts = new HashMap<>();
        identities = new Identities();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
        appBarMain = findViewById(R.id.app_bap_main);

        initDrawer();

        doSetupForFileFragment(filesFragment);

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
                        //TODO: UPLOAD
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
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, filesFragment)
                        .addToBackStack(null)
                        .commit();
                break;
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(new ResourceReadyReceiver(),
                new IntentFilter(QabelBoxApplication.RESOURCES_INITIALIZED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activeIdentity != null) {
            QabelBoxApplication.setLastActiveIdentityID(activeIdentity.getPersistenceID());
        }
    }

    private void delete(final BoxObject boxObject) {
        new AlertDialog.Builder(self)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(String.format(
                        getResources().getString(R.string.confirm_delete_message), boxObject.name))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected void onCancelled() {
                                filesFragment.setIsLoading(false);
                            }

                            @Override
                            protected void onPreExecute() {
                                filesFragment.setIsLoading(true);
                            }

                            @Override
                            protected Void doInBackground(Void... params) {
                                try {
                                    filesFragment.getBoxNavigation().delete(boxObject);
                                    filesFragment.getBoxNavigation().commit();
                                } catch (QblStorageException e) {
                                    Log.e(TAG, "Cannot delete " + boxObject.name);
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                refresh();
                            }
                        }.execute();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showAbortMessage();
                    }
                }).create().show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        } else {
            Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            if (fragment instanceof FilesFragment) {
                if (!filesFragment.browseToParent()) {
                    getFragmentManager().popBackStack();
                }
            }
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
        } else if (id == R.id.nav_browse) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, filesFragment)
                    .commit();
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
                            BoxProvider.AUTHORITY, folderId + name);

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
                    Log.e(TAG, "Failed creating folder "+ name, e);
                }
                return null;
            }

            @Override
            protected void onCancelled() {
                filesFragment.setIsLoading(false);
                showAbortMessage();
            }

            @Override
            protected void onPreExecute() {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, filesFragment)
                        .addToBackStack(null)
                        .commit();
                filesFragment.setIsLoading(true);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                refresh();
            }
        }.execute();
    }

    private void selectContactsFragment() {
        fab.show();
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

    private void selectIdentityFragment() {
        if (identities == null || identities.getIdentities() == null ||
                identities.getIdentities().isEmpty()) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddIdentityFragment())
                    .commit();
        }
    }

    @Override
    public void startAddContact(Identity identity) {
        fab.hide();
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, AddContactFragment.newInstance(identity))
                .commit();
    }

    public void selectIdentity(Identity identity) {
        activeIdentity = identity;
        selectContactsFragment();

        textViewSelectedIdentity.setText(identity.getAlias());
    }

    public void startAddIdentity() {
        fab.hide();
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

        textViewSelectedIdentity.setText(activeIdentity.getAlias());

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, filesFragment)
                .addToBackStack(null)
                .commit();    }

    @Override
    public void cancelAddIdentity() {
        browseTo(null, null, null);
    }

    @Override
    public void onNewPasswordEntered(char[] newPassword) {
        ((QabelBoxApplication) getApplication()).init(newPassword);
        setDrawerLocked(false);
        startAddIdentity();
    }

    @Override
    public void onPasswordEntered(char[] password) {
        if (((QabelBoxApplication) getApplication()).init(password)) {
            setDrawerLocked(false);
            if (QabelBoxApplication.getLastActiveIdentityID().equals("")) {
                startAddIdentity();
            } else {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, filesFragment)
                        .addToBackStack(null)
                        .commit();            }
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

    @Override
    public void onScrolledToBottom(boolean scrolledToBottom) {
        if (scrolledToBottom) {
            fab.hide();
        } else {
            fab.show();
        }
    }

    @Override
    /**
     * Handle an export request sent from the FilesFragment
     */
    public void onExport(BoxNavigation boxNavigation, BoxObject boxObject) {
        String path = boxNavigation.getPath(boxObject);
        String documentId = boxVolume.getDocumentId(path);
        Uri uri = DocumentsContract.buildDocumentUri(
                BoxProvider.AUTHORITY, documentId);
        exportUri = uri;

        // Chose a suitable place for this file, determined by the mime type
        String type = URLConnection.guessContentTypeFromName(uri.toString());
        if (type == null) {
            type = FALLBACK_MIMETYPE;
        }
        Log.i(TAG, "Mime type: " + type);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_TITLE, boxObject.name);
        intent.setDataAndType(uri, type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // the activity result will handle the actual file copy
        startActivityForResult(intent, REQUEST_CODE_CHOOSE_EXPORT);
    }

    @Override
    public void onDoRefresh(final FilesFragment filesFragment, final BoxNavigation boxNavigation, final FilesAdapter filesAdapter) {
        if (boxNavigation == null) {
            Log.e(TAG, "Refresh failed because the boxNavigation object is null");
            return;
        }
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                //TODO: Duplication with browseTo
                filesAdapter.clear();
                try {
					boxNavigation.reload();
                    for (BoxFolder boxFolder : boxNavigation.listFolders()) {
                        Log.d(TAG, "Adding folder: " + boxFolder.name);
                        filesAdapter.add(boxFolder);
                    }
                    for (BoxExternal boxExternal : boxNavigation.listExternals()) {
                        Log.d("MainActivity", "Adding external: " + boxExternal.name);
                        filesAdapter.add(boxExternal);
                    }
                    for (BoxFile boxFile : boxNavigation.listFiles()) {
                        Log.d(TAG, "Adding file: " + boxFile.name);
                        filesAdapter.add(boxFile);
                    }
                } catch (QblStorageException e) {
                    Log.e(TAG, "refresh failed", e);
                }
                return null;
            }

            @Override
            protected void onCancelled() {
				filesFragment.setIsLoading(true);
                showAbortMessage();
            }

            @Override
            protected void onPreExecute() {
                filesFragment.setIsLoading(true);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                filesAdapter.sort();
                filesAdapter.notifyDataSetChanged();

                filesFragment.setIsLoading(false);
            }
        };
        asyncTask.execute();
    }

    private void setDrawerLocked(boolean locked) {
        if (locked) {
            drawer.setDrawerListener(null);
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            toggle.setDrawerIndicatorEnabled(false);
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            toggle.setDrawerIndicatorEnabled(true);
        }
    }

    private void initDrawer() {
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        setDrawerLocked(true);

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
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

        imageViewExpandIdentity = (ImageView) navigationView.findViewById(R.id.imageViewExpandIdentity);
        imageViewExpandIdentity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (identityMenuExpanded) {
                    imageViewExpandIdentity.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp);
                    navigationView.getMenu().clear();
                    navigationView.inflateMenu(R.menu.activity_main_drawer);
                    identityMenuExpanded = false;
                } else {
                    imageViewExpandIdentity.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp);
                    navigationView.getMenu().clear();
                    for (final Identity identity : identities.getIdentities()) {
                        navigationView.getMenu()
                                .add(NAV_GROUP_IDENTITIES, Menu.NONE, Menu.NONE, identity.getAlias())
                                .setIcon(R.drawable.ic_perm_identity_black_24dp)
                                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        drawer.closeDrawer(GravityCompat.START);
                                        selectIdentity(identity);
                                        return true;
                                    }
                                });
                    }
                    navigationView.getMenu()
                            .add(NAV_GROUP_IDENTITY_ACTIONS, Menu.NONE, Menu.NONE, R.string.add_identity)
                            .setIcon(R.drawable.ic_add_circle_black_24dp)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    drawer.closeDrawer(GravityCompat.START);
                                    startAddIdentity();
                                    return true;
                                }
                            });
                    navigationView.getMenu()
                            .add(NAV_GROUP_IDENTITY_ACTIONS, Menu.NONE, Menu.NONE, R.string.manage_identities)
                            .setIcon(R.drawable.ic_settings_black_24dp)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    drawer.closeDrawer(GravityCompat.START);
                                    Toast.makeText(self, R.string.not_implemented,
                                            Toast.LENGTH_SHORT).show();
                                    return true;
                                }
                            });
                    identityMenuExpanded = true;
                }
            }
        });

        drawer.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.activity_main_drawer);
                imageViewExpandIdentity.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp);
                identityMenuExpanded = false;
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    private void showAbortMessage() {
        Toast.makeText(self, R.string.aborted,
                Toast.LENGTH_SHORT).show();
    }

    public void doSetupForFileFragment(final FilesFragment filesFragment) {

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fab.hide();
                final NewFolderFragment newFolderFragment = new NewFolderFragment();
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        newFolderFragment.setBoxNavigation(filesFragment.getBoxNavigation());
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
        filesFragment.setIsLoading(false);
    }

    private void refresh() {
        onDoRefresh(filesFragment, filesFragment.getBoxNavigation(), filesFragment.getFilesAdapter());
    }

}
