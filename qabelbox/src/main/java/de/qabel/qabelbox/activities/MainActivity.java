package de.qabel.qabelbox.activities;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.apache.commons.io.FilenameUtils;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.QblBroadcastConstants;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.account.AccountManager;
import de.qabel.qabelbox.account.AccountStatusCodes;
import de.qabel.qabelbox.fragments.BaseFragment;
import de.qabel.qabelbox.util.ShareHelper;
import de.qabel.qabelbox.communication.connection.ConnectivityManager;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.config.QabelSchema;
import de.qabel.qabelbox.dagger.HasComponent;
import de.qabel.qabelbox.dagger.components.MainActivityComponent;
import de.qabel.qabelbox.dagger.modules.ActivityModule;
import de.qabel.qabelbox.dagger.modules.MainActivityModule;
import de.qabel.qabelbox.fragments.CreateIdentityMainFragment;
import de.qabel.qabelbox.fragments.IdentitiesFragment;
import de.qabel.qabelbox.fragments.QRcodeFragment;
import de.qabel.qabelbox.helper.AccountHelper;
import de.qabel.qabelbox.helper.CacheFileHelper;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.helper.Sanity;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.navigation.MainNavigator;
import de.qabel.qabelbox.settings.SettingsActivity;
import de.qabel.qabelbox.ui.views.DrawerNavigationView;
import de.qabel.qabelbox.ui.views.DrawerNavigationViewHolder;
import kotlin.NotImplementedError;

public class MainActivity extends CrashReportingActivity
        implements IdentitiesFragment.IdentityListListener,
        HasComponent<MainActivityComponent>, NavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_SETTINGS = 17;
    private static final int REQUEST_CREATE_IDENTITY = 16;
    public static final int REQUEST_EXPORT_IDENTITY = 18;
    public static final int REQUEST_EXTERN_VIEWER_APP = 19;
    public static final int REQUEST_EXTERN_SHARE_APP = 20;

    private static final String TAG = "BoxMainActivity";

    public static final int REQUEST_EXPORT_IDENTITY_AS_CONTACT = 19;

    private static final int NAV_GROUP_IDENTITIES = 1;
    private static final int NAV_GROUP_IDENTITY_ACTIONS = 2;

    // Intent extra to specify if the files fragment should be started
    // Defaults to true and is used in tests to shortcut the activity creation
    public static final String START_FILES_FRAGMENT = "START_FILES_FRAGMENT";
    public static final String START_CONTACTS_FRAGMENT = "START_CONTACTS_FRAGMENT";
    public static final String ACTIVE_IDENTITY = "ACTIVE_IDENTITY";
    public static final String ACTIVE_CONTACT = "ACTIVE_CONTACT";
    public static final String START_FILES_FRAGMENT_PATH = "START_FILES_FRAGMENT_PATH";
    // Intent extra to specify that the activity is in test mode which disables auto-refresh
    public static final String TEST_RUN = "TEST_RUN";


    public boolean TEST = false;

    public ActionBarDrawerToggle toggle;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    DrawerNavigationView navigationView;
    @BindView(R.id.fab)
    public FloatingActionButton fab;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.app_bap_main)
    public View appBarMain;
    private MainActivity self;
    private boolean identityMenuExpanded;

    private LightingColorFilter mDrawerIndicatorTintFilter;

    @Inject
    ConnectivityManager connectivityManager;

    private DrawerNavigationViewHolder drawerHolder;

    @Inject
    Identity activeIdentity;

    @Inject
    IdentityRepository identityRepository;
    @Inject
    public ContactRepository contactRepository;

    @Inject
    AppPreference appPreferences;

    @Inject
    MainNavigator navigator;

    @Inject
    AccountManager accountManager;

    private MainActivityComponent component;

    private BroadcastReceiver accountBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int statusCode = intent.getIntExtra(QblBroadcastConstants.STATUS_CODE_PARAM, -1);
            switch (statusCode) {
                case AccountStatusCodes.LOGOUT:
                    navigator.selectCreateAccountActivity();
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "On Activity result");
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CREATE_IDENTITY) {
                if (data != null && data.hasExtra(CreateIdentityActivity.P_IDENTITY)) {
                    Identity identity = (Identity) data.getSerializableExtra(CreateIdentityActivity.P_IDENTITY);
                    if (identity == null) {
                        Log.w(TAG, "Recieved data with identity null");
                    }
                    addIdentity(identity);
                    return;
                }
            }
        }
        Log.d(TAG, "super.onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        component = getApplicationComponent()
                .plus(new ActivityModule(this))
                .plus(new MainActivityModule(this));
        component.inject(this);
        self = this;
        Log.d(TAG, "onCreate " + this.hashCode());
        registerReceiver(accountBroadCastReceiver,
                new IntentFilter(QblBroadcastConstants.Account.ACCOUNT_CHANGED));

        try {
            if (Sanity.startWizardActivities(this, identityRepository.findAll())) {
                Log.d(TAG, "started wizard dialog");
                return;
            }
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        View header = navigationView.getHeaderView(0);
        drawerHolder = new DrawerNavigationViewHolder(header);

        setSupportActionBar(toolbar);
        mDrawerIndicatorTintFilter = new LightingColorFilter(0, getResources().getColor(R.color.tintDrawerIndicator));


        installConnectivityManager();
        addBackStackListener();

        setupAccount();
        initDrawer();
        handleIntent(getIntent());
    }

    private void setupAccount() {
        AccountHelper.createSyncAccount(getApplicationContext());
        AccountHelper.configurePeriodicPolling();
    }

    public void installConnectivityManager(ConnectivityManager connectivityManager) {
        this.connectivityManager = connectivityManager;
        installConnectivityManager();
    }

    public void installConnectivityManager() {
        connectivityManager.setListener(new ConnectivityManager.ConnectivityListener() {

            private AlertDialog offlineIndicator;

            @Override
            public void handleConnectionLost() {
                if (offlineIndicator == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(self);
                    builder.setTitle(R.string.no_connection)
                            .setIcon(R.drawable.information)
                            .setNegativeButton(R.string.close_app, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    self.finishAffinity();
                                }
                            })
                            .setPositiveButton(R.string.retry_action, null);
                    offlineIndicator = builder.create();
                    offlineIndicator.setCancelable(false);
                    offlineIndicator.show();
                    offlineIndicator.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (connectivityManager.isConnected()) {
                                offlineIndicator.dismiss();
                                offlineIndicator = null;
                            }
                        }
                    });
                } else {
                    offlineIndicator.show();
                }
            }

            @Override
            public void handleConnectionEstablished() {
                if (offlineIndicator != null && offlineIndicator.isShowing()) {
                    offlineIndicator.dismiss();
                    offlineIndicator = null;
                }
            }

            @Override
            public void onDestroy() {
                if (this.offlineIndicator != null) {
                    this.offlineIndicator.dismiss();
                }
            }
        });
    }

    public void handleMainFragmentChange() {
        // Set FAB visibility according to currently visible fragment
        Fragment activeFragment = getFragmentManager().findFragmentById(R.id.fragment_container);

        if (activeFragment instanceof BaseFragment) {
            BaseFragment fragment = ((BaseFragment) activeFragment);
            toolbar.setTitle(fragment.getTitle());
            if (fragment.isFabNeeded()) {
                fab.show();
            } else {
                fab.hide();
            }
            if (!fragment.supportSubtitle()) {
                toolbar.setSubtitle(null);
            } else {
                fragment.updateSubtitle();
            }
        }
    }

    private void addBackStackListener() {

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                MainActivity.this.handleMainFragmentChange();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        Log.i(TAG, "Intent action: " + action);

        TEST = intent.getBooleanExtra(TEST_RUN, false);

        // Checks if a fragment should be launched
        boolean startFilesFragment = intent.getBooleanExtra(START_FILES_FRAGMENT, true);
        boolean startContactsFragment = intent.getBooleanExtra(START_CONTACTS_FRAGMENT, false);
        String activeContact = intent.getStringExtra(ACTIVE_CONTACT);
        if (type != null && intent.getAction() != null) {
            String scheme = intent.getScheme();

            switch (intent.getAction()) {
                case Intent.ACTION_VIEW:
                    if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0 || scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                        handleActionViewResolver(intent);

                    }
                    break;
                case Intent.ACTION_SEND:
                    Log.i(TAG, "Action send in main activity");
                    Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (imageUri != null) {
                        ArrayList<Uri> data = new ArrayList<Uri>();
                        data.add(imageUri);
                        //shareIntoApp(data, intent);
                    }

                    break;
                default:
                    if (startContactsFragment) {
                        navigator.selectContactsFragment();
                        navigator.selectChatFragment(activeContact);
                    } else if (startFilesFragment) {
                        navigator.selectFilesFragment();
                    }
                    break;
            }
        } else {
            if (startContactsFragment) {
                navigator.selectContactsFragment();
                navigator.selectChatFragment(activeContact);
            } else if (startFilesFragment) {
                navigator.selectFilesFragment();
            }
        }
    }

    public Identity getActiveIdentity() {
        return activeIdentity;
    }

    /**
     * handle open view resolver to open the correct import tool
     *
     * @param intent
     */
    private void handleActionViewResolver(Intent intent) {
        final Uri uri = intent.getData();
        String realPath;
        //check if schema content, then get real path and filename
        if (ContentResolver.SCHEME_CONTENT.compareTo(intent.getScheme()) == 0) {
            realPath = FileHelper.getRealPathFromURI(self, uri);
            if (realPath == null) {
                realPath = uri.toString();
                Log.d(TAG, "can't get real path. try to use uri " + realPath);
            }
        } else {
            //schema is file
            realPath = intent.getDataString();
        }
        String extension = FilenameUtils.getExtension(realPath);

        //grep spaces from extension like test.qco (1)
        if (extension != null && extension.length() > 0) {
            extension = extension.split(" ")[0];
            if (QabelSchema.FILE_SUFFIX_CONTACT.equals(extension)) {
                //TODO
                throw new NotImplementedError();
            } else if (QabelSchema.FILE_SUFFIX_IDENTITY.equals(extension)) {
                if (
                        new CreateIdentityMainFragment().importIdentity(self, intent)) {
                    UIHelper.showDialogMessage(self, R.string.infos, R.string.idenity_imported);
                }
            } else {
                UIHelper.showDialogMessage(this, R.string.infos, R.string.cant_import_file_type_is_unknown);
            }
        }
    }

    @OnClick(R.id.fab)
    public void floatingActionButtonClick() {
        Fragment activeFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
        String activeFragmentTag = activeFragment.getTag();
        if (activeFragment instanceof BaseFragment) {
            BaseFragment bf = (BaseFragment) activeFragment;
            //call fab action in basefragment. if fragment handled this, we are done
            if (bf.handleFABAction()) {
                return;
            }
        }
        switch (activeFragmentTag) {
            case MainNavigator.TAG_MANAGE_IDENTITIES_FRAGMENT:
                selectAddIdentityFragment();
                break;

            default:
                Log.e(TAG, "Unknown FAB action for fragment tag: " + activeFragmentTag);
        }
    }
    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment activeFragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            if (activeFragment == null) {
                super.onBackPressed();
                return;
            }
            if (activeFragment.getTag() == null) {
                getFragmentManager().popBackStack();
            } else {
                switch (activeFragment.getTag()) {
                    case MainNavigator.TAG_CONTACT_LIST_FRAGMENT:
                        super.onBackPressed();
                        break;
                    default:
                        if (getFragmentManager().getBackStackEntryCount() > 0) {
                            getFragmentManager().popBackStack();
                        } else {
                            finishAffinity();
                        }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
      //  getMenuInflater().inflate(R.menu.ab_main, menu);
        return true;
    }

    public void selectIdentity(Identity identity) {
        changeActiveIdentity(identity);
    }

    public void addIdentity(Identity identity) {

        try {
            identityRepository.save(identity);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
        changeActiveIdentity(identity, null);
        Snackbar.make(appBarMain, "Added identity: " + identity.getAlias(), Snackbar.LENGTH_LONG)
                .show();
        navigator.selectFilesFragment();
    }

    private void changeActiveIdentity(Identity identity) {
        changeActiveIdentity(identity, null);
    }

    private void changeActiveIdentity(Identity identity, @Nullable Intent intent) {
        if (activeIdentity == null || !identity.equals(activeIdentity)) {
            appPreferences.setLastActiveIdentityKey(identity.getKeyIdentifier());
            if (intent == null) {
                intent = new Intent(this, MainActivity.class);
            }
            intent.putExtra(ACTIVE_IDENTITY, identity.getKeyIdentifier());
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            startActivity(intent);
        }
    }
    @Override
    public void deleteIdentity(Identity identity) {
        try {
            identityRepository.delete(identity);
            if (identityRepository.findAll().getIdentities().size() == 0) {
                UIHelper.showDialogMessage(this, R.string.dialog_headline_info,
                        R.string.last_identity_delete_create_new, new DialogInterface.OnClickListener() {
                                                                                          @Override
                                                                                          public void onClick(DialogInterface dialog, int which) {
                                MainActivity.this.selectAddIdentityFragment();
                        }
                        });
            } else {
                try {
                    changeActiveIdentity(identityRepository.findAll().getIdentities().iterator().next());
                } catch (PersistenceException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void modifyIdentity(Identity identity) {
        try {
            identityRepository.save(identity);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
        drawerHolder.textViewSelectedIdentity.setText(getActiveIdentity().getAlias());
    }

    @Override
    protected void onDestroy() {

        if (isTaskRoot()) {
            new CacheFileHelper().freeCacheAsynchron(QabelBoxApplication.getInstance().getApplicationContext());
        }

        if (connectivityManager != null) {
            connectivityManager.onDestroy();
        }

        unregisterReceiver(accountBroadCastReceiver);

        super.onDestroy();
    }

    public void selectIdentityLayoutClick() {

        if (identityMenuExpanded) {
            drawerHolder.imageViewExpandIdentity.setImageResource(R.drawable.menu_down);
            drawerHolder.imageViewExpandIdentity.setColorFilter(mDrawerIndicatorTintFilter);
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.activity_main_drawer);
            identityMenuExpanded = false;
        } else {
            drawerHolder.imageViewExpandIdentity.setImageResource(R.drawable.menu_up);
            drawerHolder.imageViewExpandIdentity.setColorFilter(mDrawerIndicatorTintFilter);
            navigationView.getMenu().clear();
            List<Identity> identityList;
            try {
                identityList = new ArrayList<>(
                        identityRepository.findAll().getIdentities());
            } catch (PersistenceException e) {
                Log.e(TAG, "Could not list identities", e);
                identityList = new ArrayList<>();
            }
            Collections.sort(identityList, new Comparator<Identity>() {
                @Override
                public int compare(Identity lhs, Identity rhs) {
                    return lhs.getAlias().compareTo(rhs.getAlias());
                }
            });
            for (final Identity identity : identityList) {
                navigationView.getMenu()
                        .add(NAV_GROUP_IDENTITIES, Menu.NONE, Menu.NONE, identity.getAlias())
                        .setIcon(R.drawable.account)
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                drawer.closeDrawer(GravityCompat.START);
                                MainActivity.this.selectIdentity(identity);
                                return true;
                            }
                        });
            }
            navigationView.getMenu()
                    .add(NAV_GROUP_IDENTITY_ACTIONS, Menu.NONE, Menu.NONE, R.string.add_identity)
                    .setIcon(R.drawable.plus_circle)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            drawer.closeDrawer(GravityCompat.START);
                            MainActivity.this.selectAddIdentityFragment();
                            return true;
                        }
                    });
            navigationView.getMenu()
                    .add(NAV_GROUP_IDENTITY_ACTIONS, Menu.NONE, Menu.NONE, R.string.manage_identities)
                    .setIcon(R.drawable.settings)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            drawer.closeDrawer(GravityCompat.START);
                            navigator.selectManageIdentitiesFragment();
                            return true;
                        }
                    });
            navigationView.getMenu()
                    .add(NAV_GROUP_IDENTITY_ACTIONS, Menu.NONE, Menu.NONE, R.string.logout)
                    .setIcon(R.drawable.account_off)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            UIHelper.showConfirmationDialog(self, R.string.logout,
                                    R.string.logout_confirmation, R.drawable.account_off,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            accountManager.logout();
                                        }
                                    });
                            return true;
                        }
                    });
            identityMenuExpanded = true;
        }
    }

    private void initDrawer() {
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        drawerHolder.qabelLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.closeDrawer(GravityCompat.START);
                showQRCode(self, MainActivity.this.getActiveIdentity());
            }
        });

        drawerHolder.selectIdentityLayout.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MainActivity.this.selectIdentityLayoutClick();
                    }
                });

        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        toggle.setDrawerIndicatorEnabled(true);
        navigationView.setNavigationItemSelectedListener(this);

        // Map QR-Code indent to alias textview in nav_header_main
        String boxName = new AppPreference(self).getAccountName();
        drawerHolder.textViewBoxAccountName.setText(boxName != null ? boxName : getString(R.string.app_name));

        drawerHolder.imageViewExpandIdentity.setColorFilter(mDrawerIndicatorTintFilter);

        drawerHolder.textViewSelectedIdentity.setText(activeIdentity.getAlias());
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
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
                drawerHolder.imageViewExpandIdentity.setImageResource(R.drawable.menu_down);
                identityMenuExpanded = false;
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    public static void showQRCode(MainActivity activity, Identity identity) {
        activity.getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, QRcodeFragment.newInstance(identity), null)
                .addToBackStack(null)
                .commit();
    }

    private void selectAddIdentityFragment() {

        Intent i = new Intent(self, CreateIdentityActivity.class);
        int identitiesCount;
        try {
            identitiesCount = identityRepository.findAll().getIdentities().size();
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
        i.putExtra(CreateIdentityActivity.FIRST_RUN, identitiesCount == 0);

        if (identitiesCount == 0) {
            finish();
            self.startActivity(i);
        } else {
            self.startActivityForResult(i, REQUEST_CREATE_IDENTITY);
        }
    }

    @Override
    public MainActivityComponent getComponent() {
        return component;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_tellafriend) {
            ShareHelper.tellAFriend(this);
        }
        if (id == R.id.nav_contacts) {
            navigator.selectContactsFragment();
        } else if (id == R.id.nav_browse) {
            navigator.selectFilesFragment();
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, REQUEST_SETTINGS);
        } else if (id == R.id.nav_about) {
            navigator.selectAboutFragment();
        } else if (id == R.id.nav_help) {
            navigator.selectHelpFragment();
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
