package de.qabel.qabelbox.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;

import de.qabel.qabelbox.exceptions.QblStorageException;
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
                        NewFolderFragment.OnFragmentInteractionListener {

    private static final String TAG = "BoxMainActivity";
    private static final int REQUEST_CODE_OPEN = 11;
    private static final int REQUEST_CODE_UPLOAD_FILE = 12;
    public static final String HARDCODED_ROOT = BoxProvider.PUB_KEY
            + BoxProvider.DOCID_SEPARATOR + BoxProvider.BUCKET + BoxProvider.DOCID_SEPARATOR
            + BoxProvider.PREFIX + BoxProvider.DOCID_SEPARATOR + BoxProvider.PATH_SEP;
    private static final int REQUEST_CODE_DELETE_FILE = 13;
    public static final int DIRECTORY_LOADER = 0;
    private BoxVolume boxVolume;
    private BoxProvider provider;
    private FloatingActionButton fab;


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
        provider = ((QabelBoxApplication) getApplication()).getProvider();
        Log.i(TAG, "Provider: " + provider);
        boxVolume = provider.getVolumeForRoot(null, null, null);

        fab = (FloatingActionButton) findViewById(R.id.fab);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Check if activity is started with ACTION_SEND or ACTION_SEND_MULTIPLE
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        Log.i(TAG, "Intent action: " + action);

       if (Intent.ACTION_SEND.equals(action) && type != null) {
           Log.i(TAG, "Action send in main activity");
           Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
           if (imageUri != null) {
               browseTo(null, imageUri);
           }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
           ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
           //TODO: Implement
        } else {
           browseTo(null, null);
        }
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

        if (id == R.id.nav_browse) {
            Bundle bundle = new Bundle();
            bundle.putString("dir", HARDCODED_ROOT);
            getLoaderManager().initLoader(DIRECTORY_LOADER, bundle, null);
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
}
