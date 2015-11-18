package de.qabel.qabelbox.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import org.apache.commons.io.IOUtils;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;

import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import de.qabel.core.storage.BoxFile;
import de.qabel.core.storage.BoxFolder;
import de.qabel.core.storage.BoxNavigation;
import de.qabel.core.storage.BoxObject;
import de.qabel.core.storage.BoxVolume;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.filesystem.BoxContentObserver;
import de.qabel.qabelbox.fragments.FilesFragment;
import de.qabel.qabelbox.fragments.NewFolderFragment;
import de.qabel.qabelbox.fragments.SelectUploadFolderFragment;
import de.qabel.qabelbox.providers.BoxContentProvider;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    SelectUploadFolderFragment.OnSelectedUploadFolderListener,
                        NewFolderFragment.OnFragmentInteractionListener {

    private Account boxAccount;
    private BoxVolume boxVolume;
    private BoxNavigation boxNavigation;

    private Cursor getFolder(int folderID) {
        return getContentResolver().query(
                Uri.parse(BoxContentProvider.PREFIX_CONTENT + BoxContentProvider.AUTHORITY + BoxContentProvider.SUFFIX_FOLDER),
                new String[]{BoxContentProvider.ROW_ID, BoxContentProvider.ROW_TYPE, BoxContentProvider.ROW_NAME},
                BoxContentProvider.ROW_PARENT + " = ?",
                new String[]{String.valueOf(folderID)},
                null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        boxAccount = createSyncAccount(this);

        setupContentProvider();

        requestManualSync();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new NewFolderFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

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

       if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, genUploadFragment(null, imageUri))
                        .addToBackStack(null)
                        .commit();
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (imageUris != null) {
                for (Uri imageUri : imageUris) {
                    // TODO: Implement multi upload
                }
            }
        } else {
           //getFragmentManager().beginTransaction()
           //        .replace(R.id.fragment_container, genFilesFragment(null))
           //        .addToBackStack(null)
           //        .commit();
        }
    }

    private void requestManualSync() {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        ContentResolver.requestSync(boxAccount, BoxContentProvider.AUTHORITY, settingsBundle);
    }

    private void setupContentProvider() {
        ContentResolver contentResolver = getContentResolver();
        ContentResolver.setSyncAutomatically(boxAccount, BoxContentProvider.AUTHORITY, true);
        contentResolver.registerContentObserver(Uri.parse(BoxContentProvider.PREFIX_CONTENT + BoxContentProvider.AUTHORITY), true, new BoxContentObserver(null));
        contentResolver.notifyChange(Uri.parse(BoxContentProvider.PREFIX_CONTENT + BoxContentProvider.AUTHORITY), null);
    }

    public static Account createSyncAccount(Context context) {
        // TODO: Remove hardcoded account name
        Account newAccount = new Account("Box", BoxContentProvider.ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);

        // TODO: Handle error case
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
        } else {
        }

        return  newAccount;
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

    private FilesFragment genFilesFragment(final BoxFolder navigateTo) {
        final ArrayList<BoxObject> boxObjects = new ArrayList<>();
        try {
            if (navigateTo != null) {
                boxNavigation = boxNavigation.navigate(navigateTo);
            }
            for (BoxFolder boxFolder : boxNavigation.listFolders()){
                Log.d("MainActivity", "Adding folder: " + boxFolder.name);
                boxObjects.add(boxFolder);
            }
            for (BoxFile boxFile : boxNavigation.listFiles()) {
                Log.d("MainActivity", "Adding file: " + boxFile.name);
                boxObjects.add(boxFile);
            }
        } catch (QblStorageException e) {
            e.printStackTrace();
        }

        Collections.sort(boxObjects);

        FilesFragment filesFragment = new FilesFragment();

        final FilesAdapter filesAdapter = new FilesAdapter(this, boxObjects);
        filesAdapter.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                BoxObject boxObject = boxObjects.get(position);
                if (boxObject instanceof BoxFolder) {
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, genFilesFragment(((BoxFolder) boxObject)))
                                    .addToBackStack(null)
                                    .commit();
                } else if (boxObject instanceof BoxFile){
                    try {
                        InputStream inputStream = boxNavigation.download((BoxFile) boxObject);
                        Log.d("MainActivity", "Downloaded");
                        File file = new File(getExternalFilesDir(null), boxObject.name);
                        Log.d("MainActivity", "Saving to: " + getExternalFilesDir(null).toString() + '/' + boxObject.name);
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        IOUtils.copy(inputStream, fileOutputStream);
                        inputStream.close();
                        fileOutputStream.close();
                        Intent viewIntent = new Intent();
                        Uri uriToFile = Uri.fromFile(file);
                        viewIntent.setDataAndType(Uri.fromFile(file),
                                URLConnection.guessContentTypeFromName(uriToFile.toString()));
                        startActivity(Intent.createChooser(viewIntent, "Open with"));
                    } catch (QblStorageException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        filesFragment.setAdapter(filesAdapter);
        return filesFragment;
    }

    // TODO: This method is almost equal to genFilesFragment, extract to new method.
    private SelectUploadFolderFragment genUploadFragment(final BoxFolder navigateTo, final Uri uri) {
        final ArrayList<BoxObject> boxObjects = new ArrayList<>();
        try {
            if (navigateTo != null) {
                boxNavigation = boxNavigation.navigate(navigateTo);
            }
            for (BoxFolder boxFolder : boxNavigation.listFolders()){
                Log.d("MainActivity", "Adding folder: " + boxFolder.name);
                boxObjects.add(boxFolder);
            }
            for (BoxFile boxFile : boxNavigation.listFiles()) {
                Log.d("MainActivity", "Adding file: " + boxFile.name);
                boxObjects.add(boxFile);
            }
        } catch (QblStorageException e) {
            e.printStackTrace();
        }

        Collections.sort(boxObjects);

        SelectUploadFolderFragment filesFragment = new SelectUploadFolderFragment();
        filesFragment.setUri(uri);
        final FilesAdapter filesAdapter = new FilesAdapter(this, boxObjects);
        filesAdapter.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                BoxObject boxObject = boxObjects.get(position);
                if (boxObject instanceof BoxFolder) {
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, genUploadFragment(((BoxFolder) boxObject), uri))
                            .addToBackStack(null)
                            .commit();
                }
            }
        });

        filesFragment.setAdapter(filesAdapter);
        return filesFragment;
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camara) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onFolderSelected(Uri uri) {

        ParcelFileDescriptor inputPFD;
        String name;
        try {
            inputPFD = getContentResolver().openFileDescriptor(uri, "r");
            Cursor returnCursor =
                    getContentResolver().query(uri, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            name = returnCursor.getString(nameIndex);
            returnCursor.close();

        } catch (FileNotFoundException e) {
            Log.e("BOX", "File not found: " + uri);
            finish();
            return;
        }

        try {
            InputStream content = new ParcelFileDescriptor.AutoCloseInputStream(inputPFD);
            boxNavigation.upload(name, content);
            boxNavigation.commit();
        } catch (QblStorageException e) {
            Log.e("BOX", "Upload failed", e);
        }

        finish();

//        final int id = 1;
//        final NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
//        mBuilder.setContentTitle("Uploading " + uri.getLastPathSegment())
//                .setContentText("Upload in progress")
//                .setSmallIcon(R.drawable.notification_template_icon_bg);
//        new Thread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        int incr;
//                        for (incr = 0; incr <= 100; incr+=5) {
//                            mBuilder.setProgress(100, incr, false);
//                            mNotifyManager.notify(id, mBuilder.build());
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                Log.d("asd", "asd");
//                            }
//                        }
//                        mBuilder.setContentText("Upload complete")
//                                .setProgress(0,0,false);
//                        mNotifyManager.notify(id, mBuilder.build());
//                    }
//                }
//        ).start();
//        finish();
    }

    @Override
    public void onAbort() {

    }

    @Override
    public void onCreateFolder(String name) {
        try {
            boxNavigation.createFolder(name);
            boxNavigation.commit();
        } catch (QblStorageException e) {
            e.printStackTrace();
        }
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, genFilesFragment(null))
                .addToBackStack(null)
                .commit();
    }
}
