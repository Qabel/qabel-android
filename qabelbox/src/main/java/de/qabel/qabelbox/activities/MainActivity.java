package de.qabel.qabelbox.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.bottomsheet.BottomSheet;
import com.google.zxing.integration.android.IntentIntegrator;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.fragments.AddContactFragment;
import de.qabel.qabelbox.fragments.AddIdentityFragment;
import de.qabel.qabelbox.fragments.BaseFragment;
import de.qabel.qabelbox.fragments.ContactFragment;
import de.qabel.qabelbox.fragments.FilesFragment;
import de.qabel.qabelbox.fragments.IdentitiesFragment;
import de.qabel.qabelbox.fragments.SelectUploadFolderFragment;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxExternal;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxVolume;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    SelectUploadFolderFragment.OnSelectedUploadFolderListener,
                            ContactFragment.ContactListListener,
                                    AddIdentityFragment.AddIdentityListener,
                                                AddContactFragment.AddContactListener,
                                                    FilesFragment.FilesListListener,
                                                        IdentitiesFragment.IdentityListListener {

    private static final String TAG_FILES_FRAGMENT = "TAG_FILES_FRAGMENT";
    private static final String TAG_CONTACT_LIST_FRAGMENT = "TAG_CONTACT_LIST_FRAGMENT";
    private static final String TAG_MANAGE_IDENTITIES_FRAGMENT = "TAG_MANAGE_IDENTITIES_FRAGMENT";
    private static final String TAG_ADD_IDENTITY_FRAGMENT = "TAG_ADD_IDENTITY_FRAGMENT";
    private static final String TAG_ADD_CONTACT_FRAGMENT = "TAG_ADD_CONTACT_FRAGMENT";

    private static final String TAG = "BoxMainActivity";
    private static final int REQUEST_CODE_OPEN = 11;
    private static final int REQUEST_CODE_UPLOAD_FILE = 12;
    public static final String HARDCODED_ROOT = BoxProvider.DOCID_SEPARATOR
            + BoxProvider.BUCKET + BoxProvider.DOCID_SEPARATOR
            + BoxProvider.PREFIX + BoxProvider.DOCID_SEPARATOR + BoxProvider.PATH_SEP;
    private static final int REQUEST_CODE_DELETE_FILE = 13;
    private static final int REQUEST_CODE_CHOOSE_EXPORT = 14;
    private static final String FALLBACK_MIMETYPE = "application/octet-stream";
    private static final int NAV_GROUP_IDENTITIES = 1;
    private static final int NAV_GROUP_IDENTITY_ACTIONS = 2;
    private DrawerLayout drawer;
    private BoxVolume boxVolume;
    private ActionBarDrawerToggle toggle;
    private BoxProvider provider;
    public FloatingActionButton fab;
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
    private LocalQabelService mService;

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
            String path = "";
            if (filesFragment != null) {
                BoxNavigation boxNavigation = filesFragment.getBoxNavigation();
                if (boxNavigation != null) {
                    path = boxNavigation.getPath();
                }
            }
            uploadUri(uri, path);
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

    private boolean uploadUri(Uri uri, String targetFolder) {
        Toast.makeText(self, R.string.uploading_file,
                Toast.LENGTH_SHORT).show();
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "No valid url for uploading" + uri);
            return true;
        }
        cursor.moveToFirst();
        String displayName = cursor.getString(
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        Log.i(TAG, "Displayname: " + displayName);
        String keyIdentifier = mService.getActiveIdentity().getEcPublicKey()
                .getReadableKeyIdentifier();
        Uri uploadUri = DocumentsContract.buildDocumentUri(
                BoxProvider.AUTHORITY, keyIdentifier + HARDCODED_ROOT + targetFolder + displayName);
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
        appBarMain = findViewById(R.id.app_bap_main);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        self = this;
        initFloatingActionButton();

        Intent serviceIntent = new Intent(this, LocalQabelService.class);
        bindService(serviceIntent, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalQabelService.LocalBinder binder = (LocalQabelService.LocalBinder) service;
                mService = binder.getService();
                onLocalServiceConnected();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }

        }, Context.BIND_AUTO_CREATE);

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                // Set FAB visibility according to currently visible fragment
                Fragment activeFragment = getFragmentManager().findFragmentById(R.id.fragment_container);

                if (activeFragment instanceof BaseFragment) {
                    BaseFragment fragment = ((BaseFragment) activeFragment);
                    toolbar.setTitle(fragment.getTitle());
                    System.out.println("base fab " + fragment.isFabNeeded());
                    if (fragment.isFabNeeded()) {
                        fab.show();
                    } else {
                        fab.hide();
                    }
                } else {
                    //@todo add isFabNeeded to baseFragment and check this value
                    switch (activeFragment.getTag()) {
                        case TAG_CONTACT_LIST_FRAGMENT:
                            fab.show();
                            break;
                        case TAG_MANAGE_IDENTITIES_FRAGMENT:
                            fab.show();
                            break;
                        case TAG_ADD_IDENTITY_FRAGMENT:
                            fab.hide();
                            break;
                        case TAG_ADD_CONTACT_FRAGMENT:
                            fab.hide();
                            break;
                        case TAG_FILES_FRAGMENT:
                        default:
                            Log.d(TAG, "No FAB action required");
                    }
                }

                //check if navigation drawer need to reset
                if (getFragmentManager().getBackStackEntryCount() == 0 || (activeFragment instanceof BaseFragment) && !((BaseFragment) activeFragment).supportBackButton()) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    toggle.setDrawerIndicatorEnabled(true);
                }
            }
        });

    }

    private void onLocalServiceConnected() {
        Log.d(TAG, "LocalQabelService connected");

        provider = ((QabelBoxApplication) getApplication()).getProvider();
        Log.i(TAG, "Provider: " + provider);

        provider.setLocalService(mService);
        initDrawer();

        Identity activeIdentity = mService.getActiveIdentity();
        if (activeIdentity != null) {
            textViewSelectedIdentity.setText(activeIdentity.getAlias());
            initFilesFragment();
            initBoxVolume(activeIdentity);
        }

        // Check if activity is started with ACTION_SEND or ACTION_SEND_MULTIPLE
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        Log.i(TAG, "Intent action: " + action);

        // Checks if a fragment should be launched
        switch (intent.getAction()) {
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
                if (activeIdentity != null) {
                    selectFilesFragment();
                } else {
                    selectAddIdentityFragment();
                }
                break;
        }

    }

    private void initBoxVolume(Identity activeIdentity) {
        boxVolume = provider.getVolumeForRoot(
                activeIdentity.getEcPublicKey().getReadableKeyIdentifier(),
                null, null);
    }

    private void initFloatingActionButton() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment activeFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
                String activeFragmentTag = activeFragment.getTag();

                switch (activeFragmentTag) {
                    case TAG_FILES_FRAGMENT:
                        filesFragmentBottomSheet();
                        break;

                    case TAG_CONTACT_LIST_FRAGMENT:
                        startAddContact(mService.getActiveIdentity());
                        break;

                    case TAG_MANAGE_IDENTITIES_FRAGMENT:
                        selectAddIdentityFragment();
                        break;

                    default:
                        Log.e(TAG, "Unknown FAB action for fragment tag: " + activeFragmentTag);
                }
            }
        });
    }

    private void filesFragmentBottomSheet() {
        new BottomSheet.Builder(self).sheet(R.menu.create_bottom_sheet)
                .listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case R.id.create_folder:
                                newFolderDialog();
                                break;
                            case R.id.upload_file:
                                Intent intentOpen = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                intentOpen.addCategory(Intent.CATEGORY_OPENABLE);
                                intentOpen.setType("*/*");
                                startActivityForResult(intentOpen, REQUEST_CODE_UPLOAD_FILE);
                                break;
                        }
                    }
                }).show();
    }

    private void newFolderDialog() {
        AlertDialog.Builder renameDialog = new AlertDialog.Builder(self);

        renameDialog.setTitle(R.string.add_folder_header);
        renameDialog.setMessage(R.string.add_folder_name);

        final EditText editTextNewFolder = new EditText(self);
        renameDialog.setView(editTextNewFolder);

        renameDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String newFolderName = editTextNewFolder.getText().toString();
                if (!newFolderName.equals("")) {
                    createFolder(newFolderName, filesFragment.getBoxNavigation());
                }
            }
        });

        renameDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        renameDialog.show();
    }

    private void initFilesFragment() {
        filesFragment = FilesFragment.newInstance(boxVolume);
        filesFragment.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                final BoxObject boxObject = filesFragment.getFilesAdapter().get(position);
                if (boxObject instanceof BoxFolder) {
                    filesFragment.browseTo(((BoxFolder) boxObject));
                } else if (boxObject instanceof BoxFile) {
                    // Open
                    showFile(boxObject);
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
    }

    /**
     * open system show file dialog
     *
     * @param boxObject
     */
    public void showFile(BoxObject boxObject) {
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

    @Override
    protected void onPause() {
        super.onPause();
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
        } else {
            Fragment activeFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            if (activeFragment.getTag() == null) {
                getFragmentManager().popBackStack();

            } else {
                switch (activeFragment.getTag()) {
                    case TAG_FILES_FRAGMENT:
                        toggle.setDrawerIndicatorEnabled(true);
                        if (!filesFragment.handleBackPressed()&&!filesFragment.browseToParent()) {
                            finishAffinity();
                        }
                        break;
                    default:
                        getFragmentManager().popBackStack();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ab_main, menu);
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
            selectFilesFragment();
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

    public void createFolder(final String name, final BoxNavigation boxNavigation) {
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
                selectFilesFragment();
                filesFragment.setIsLoading(true);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                refresh();
            }
        }.execute();
    }

    @Override
    public void startAddContact(Identity identity) {
        selectAddContactFragment(identity);
    }

    public void selectIdentity(Identity identity) {
        changeActiveIdentity(identity);

        selectContactsFragment();
    }

    @Override
    public void addIdentity(Identity identity) {
        mService.addIdentity(identity);
        changeActiveIdentity(identity);
        provider.notifyRootsUpdated();

        Snackbar.make(appBarMain, "Added identity: " + identity.getAlias(), Snackbar.LENGTH_LONG)
                .show();

        textViewSelectedIdentity.setText(identity.getAlias());
        if (filesFragment != null) {
            getFragmentManager().beginTransaction().remove(filesFragment).commit();
        }
        initBoxVolume(identity);
        initFilesFragment();

        selectFilesFragment();
    }

    private void changeActiveIdentity(Identity identity) {
        mService.setActiveIdentity(identity);
        textViewSelectedIdentity.setText(identity.getAlias());
        getFragmentManager().beginTransaction().remove(filesFragment).commit();
        initBoxVolume(identity);
        initFilesFragment();
    }

    @Override
    public void cancelAddIdentity() {
        selectFilesFragment();
    }

    @Override
    public void addContact(Contact contact) {
        mService.addContact(contact);
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
    public void deleteIdentity(Identity identity) {
        provider.notifyRootsUpdated();
        mService.deleteIdentity(identity);
    }

    @Override
    public void modifyIdentity(Identity identity) {
        provider.notifyRootsUpdated();
        mService.modifyIdentity(identity);
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

        setDrawerLocked(false);

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Map QR-Code indent to alias textview in nav_header_main
        textViewSelectedIdentity = (TextView) navigationView.findViewById(R.id.textViewSelectedIdentity);
        textViewSelectedIdentity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Identity activeIdentity = mService.getActiveIdentity();
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
                    List<Identity> identityList = new ArrayList<>(
                            mService.getIdentities().getIdentities());
                    Collections.sort(identityList, new Comparator<Identity>() {
                        @Override
                        public int compare(Identity lhs, Identity rhs) {
                            return lhs.getAlias().compareTo(rhs.getAlias());
                        }
                    });
                    for (final Identity identity : identityList) {
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
                                selectAddIdentityFragment();
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
                                selectManageIdentitiesFragment();
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

    private void refresh() {
        onDoRefresh(filesFragment, filesFragment.getBoxNavigation(), filesFragment.getFilesAdapter());
    }

    /*
        FRAGMENT SELECTION METHODS
    */

    private void selectAddContactFragment(Identity identity) {
        fab.hide();
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, AddContactFragment.newInstance(identity), TAG_ADD_CONTACT_FRAGMENT)
                .addToBackStack(null)
                .commit();
    }

    private void selectManageIdentitiesFragment() {
        fab.show();
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,
                        IdentitiesFragment.newInstance(mService.getIdentities()),
                        TAG_MANAGE_IDENTITIES_FRAGMENT)
                .addToBackStack(null)
                .commit();
    }

    private void selectContactsFragment() {
        fab.show();
        Identity activeIdentity = mService.getActiveIdentity();
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,
                        ContactFragment.newInstance(mService.getContacts(activeIdentity), activeIdentity),
                        TAG_CONTACT_LIST_FRAGMENT)
                .addToBackStack(null)
                .commit();
    }

    private void selectAddIdentityFragment() {
        fab.hide();
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AddIdentityFragment(), TAG_ADD_IDENTITY_FRAGMENT)
                .addToBackStack(null)
                .commit();
    }

    private void selectFilesFragment() {
        fab.show();
        filesFragment.setIsLoading(false);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, filesFragment, TAG_FILES_FRAGMENT)
                .commit();
    }
}
