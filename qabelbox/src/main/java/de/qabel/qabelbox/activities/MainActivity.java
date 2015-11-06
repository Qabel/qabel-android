package de.qabel.qabelbox.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
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

import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import de.qabel.ServiceConstants;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import de.qabel.core.storage.BoxNavigation;
import de.qabel.core.storage.BoxVolume;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.FilesAdapter;
import de.qabel.qabelbox.filesystem.BoxContentObserver;
import de.qabel.qabelbox.filesystem.BoxFile;
import de.qabel.qabelbox.filesystem.BoxFolder;
import de.qabel.qabelbox.filesystem.BoxObject;
import de.qabel.qabelbox.fragments.FilesFragment;
import de.qabel.qabelbox.fragments.SelectUploadFolderFragment;
import de.qabel.qabelbox.providers.BoxContentProvider;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    SelectUploadFolderFragment.OnSelectedUploadFolderListener {

    private Account boxAccount;
    private BoxVolume boxVolume;

    private Cursor getFolder(int folderID) {
        return getContentResolver().query(
                Uri.parse(BoxContentProvider.PREFIX_CONTENT + BoxContentProvider.AUTHORITY + BoxContentProvider.SUFFIX_FOLDER),
                new String[]{BoxContentProvider.ROW_ID, BoxContentProvider.ROW_TYPE, BoxContentProvider.ROW_NAME},
                BoxContentProvider.ROW_PARENT + " = ?",
                new String[]{String.valueOf(folderID)},
                null);
    }

    private ArrayList<BoxObject> parseFolder(Cursor folderCursor) {
        ArrayList<BoxObject> boxObjects = new ArrayList<>();

        if (folderCursor.moveToFirst()) {
            do {
                BoxObject boxObject;
                String type = folderCursor.getString(1);
                switch (type) {
                    case BoxContentProvider.TYPE_FILE:
                        boxObject = new BoxFile();
                        break;
                    case BoxContentProvider.TYPE_FOLDER:
                        boxObject = new BoxFolder();
                        break;
                    default:
                        continue;
                }
                boxObject.setId(folderCursor.getInt(0));
                boxObject.setName(folderCursor.getString(2));
                boxObjects.add(boxObject);
            } while (folderCursor.moveToNext());
        }
        return boxObjects;
    }

    private void genDemoBoxContent() {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            ContentValues contentValues = new ContentValues();
            if (random.nextBoolean()) {
                contentValues.put(BoxContentProvider.ROW_TYPE, BoxContentProvider.TYPE_FILE);
            } else {
                contentValues.put(BoxContentProvider.ROW_TYPE, BoxContentProvider.TYPE_FOLDER);
            }
            contentValues.put(BoxContentProvider.ROW_ID, i);
            contentValues.put(BoxContentProvider.ROW_NAME, UUID.randomUUID().toString());
            if (random.nextBoolean()) {
                contentValues.put(BoxContentProvider.ROW_PARENT, 0);
            } else {
                contentValues.put(BoxContentProvider.ROW_PARENT, i + 1 - random.nextInt(i + 1));
            }
            getContentResolver().insert(Uri.parse(BoxContentProvider.PREFIX_CONTENT + BoxContentProvider.AUTHORITY + BoxContentProvider.SUFFIX_FILE), contentValues);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AWSCredentials credentials = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return getResources().getString(R.string.aws_user);
            }

            @Override
            public String getAWSSecretKey() {
                return getResources().getString(R.string.aws_password);
            }
        };

        CryptoUtils cryptoUtils = new CryptoUtils();
        QblECKeyPair testKey = new QblECKeyPair(Hex.decode("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a"));

        boxVolume = new BoxVolume("qabel", "boxtest", credentials, testKey, cryptoUtils.getRandomBytes(16));
        try {
            boxVolume.createIndex("qabel", "boxtest");
        } catch (QblStorageException e) {
            e.printStackTrace();
        }

        try {
            BoxNavigation boxNavigation = boxVolume.navigate();
            de.qabel.core.storage.BoxFolder folder = boxNavigation.createFolder("asd");

        } catch (QblStorageException e) {
            e.printStackTrace();
        }

        boxAccount = createSyncAccount(this);

        setupContentProvider();

        genDemoBoxContent();

        requestManualSync();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

       if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, genRandomUploadFragment(imageUri))
                        .addToBackStack(null)
                        .commit();
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (imageUris != null) {
                for (Uri imageUri : imageUris) {
                    Log.wtf("asd", imageUri.toString());
                }
            }
        } else {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, genRandomFragment(0))
                    .addToBackStack(null)
                    .commit();
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
        Account newAccount = new Account("Box", BoxContentProvider.ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);

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

    private FilesFragment genRandomFragment(int id) {
        final ArrayList<BoxObject> boxObjects = parseFolder(getFolder(id));

        Collections.sort(boxObjects);

        FilesFragment filesFragment = new FilesFragment();
        final FilesAdapter filesAdapter = new FilesAdapter(this, boxObjects);
        filesAdapter.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, genRandomFragment(boxObjects.get(position).getId()))
                        .addToBackStack(null)
                        .commit();
            }
        });

        filesFragment.setAdapter(filesAdapter);
        return filesFragment;
    }

    private SelectUploadFolderFragment genRandomUploadFragment(final Uri uri) {
        Random random = new Random();
        final ArrayList<BoxObject> files = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BoxObject fileItem = new BoxObject();
            fileItem.setName(UUID.randomUUID().toString());
            fileItem.setShareCount(random.nextInt(5));
            files.add(fileItem);
        }

        Collections.sort(files);

        SelectUploadFolderFragment filesFragment = new SelectUploadFolderFragment();
        final FilesAdapter filesAdapter = new FilesAdapter(this, files);
        filesAdapter.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, genRandomUploadFragment(uri))
                        .addToBackStack("uploadFragment")
                        .commit();
            }
        });

        filesFragment.setAdapter(filesAdapter);
        filesFragment.setUri(uri);
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
        Intent uploadFileIntent = new Intent();
        uploadFileIntent.setComponent(
                new ComponentName(ServiceConstants.SERVICE_PACKAGE_NAME, "de.qabel.qabellauncher.MainActivity"));
        uploadFileIntent.setAction(ServiceConstants.ACTION_UPLOAD_FILE);
        uploadFileIntent.putExtra("uploadURI", uri.toString());
        startActivity(uploadFileIntent);

        final int id = 1;
        final NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Uploading " + uri.getLastPathSegment())
                .setContentText("Upload in progress")
                .setSmallIcon(R.drawable.notification_template_icon_bg);
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        int incr;
                        for (incr = 0; incr <= 100; incr+=5) {
                            mBuilder.setProgress(100, incr, false);
                            mNotifyManager.notify(id, mBuilder.build());
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Log.d("asd", "asd");
                            }
                        }
                        mBuilder.setContentText("Upload complete")
                                .setProgress(0,0,false);
                        mNotifyManager.notify(id, mBuilder.build());
                    }
                }
        ).start();
        finish();
    }

    @Override
    public void onAbort() {

    }
}
