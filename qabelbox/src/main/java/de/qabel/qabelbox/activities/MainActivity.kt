package de.qabel.qabelbox.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import de.qabel.core.config.Identity
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.exception.PersistenceException
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.account.AccountManager
import de.qabel.qabelbox.account.AccountStatusCodes
import de.qabel.qabelbox.communication.connection.ConnectivityManager
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.dagger.HasComponent
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.dagger.modules.ActivityModule
import de.qabel.qabelbox.dagger.modules.MainActivityModule
import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.fragments.IdentitiesFragment
import de.qabel.qabelbox.fragments.QRcodeFragment
import de.qabel.qabelbox.helper.AccountHelper
import de.qabel.qabelbox.helper.CacheFileHelper
import de.qabel.qabelbox.helper.Sanity
import de.qabel.qabelbox.helper.UIHelper
import de.qabel.qabelbox.navigation.MainNavigator
import de.qabel.qabelbox.settings.SettingsActivity
import de.qabel.qabelbox.ui.views.DrawerNavigationView
import de.qabel.qabelbox.ui.views.DrawerNavigationViewHolder
import de.qabel.qabelbox.util.ShareHelper
import org.jetbrains.anko.ctx
import java.util.*
import javax.inject.Inject

class MainActivity : CrashReportingActivity(), IdentitiesFragment.IdentityListListener, HasComponent<MainActivityComponent>, NavigationView.OnNavigationItemSelectedListener {


    var TEST = false

    lateinit var toggle: ActionBarDrawerToggle

    @BindView(R.id.drawer_layout)
    lateinit internal var drawer: DrawerLayout
    @BindView(R.id.nav_view)
    lateinit internal var navigationView: DrawerNavigationView
    @BindView(R.id.fab)
    lateinit var fab: FloatingActionButton
    @BindView(R.id.toolbar)
    lateinit internal var toolbar: Toolbar
    @BindView(R.id.app_bap_main)
    lateinit var appBarMain: View

    lateinit private var self: MainActivity
    private var identityMenuExpanded: Boolean = false

    lateinit private var mDrawerIndicatorTintFilter: LightingColorFilter

    @Inject
    lateinit internal var connectivityManager: ConnectivityManager

    lateinit private var drawerHolder: DrawerNavigationViewHolder

    @Inject
    lateinit var activeIdentity: Identity
        internal set

    @Inject
    lateinit internal var identityRepository: IdentityRepository
    @Inject
    lateinit var contactRepository: ContactRepository

    @Inject
    lateinit internal var appPreferences: AppPreference

    @Inject
    lateinit internal var navigator: MainNavigator

    @Inject
    lateinit internal var accountManager: AccountManager

    lateinit private var component: MainActivityComponent

    private val accountBroadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val statusCode = intent.getIntExtra(QblBroadcastConstants.STATUS_CODE_PARAM, -1)
            when (statusCode) {
                AccountStatusCodes.LOGOUT -> navigator.selectCreateAccountActivity()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "On Activity result")
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CREATE_IDENTITY) {
                if (data != null && data.hasExtra(CreateIdentityActivity.P_IDENTITY)) {
                    val identity = data.getSerializableExtra(CreateIdentityActivity.P_IDENTITY) as Identity
                    addIdentity(identity)
                    return
                }
            }
        }
        Log.d(TAG, "super.onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = applicationComponent.plus(ActivityModule(this)).plus(MainActivityModule(this))
        component.inject(this)
        self = this
        Log.d(TAG, "onCreate " + this.hashCode())
        registerReceiver(accountBroadCastReceiver,
                IntentFilter(QblBroadcastConstants.Account.ACCOUNT_CHANGED))

        try {
            if (Sanity.startWizardActivities(this, identityRepository.findAll())) {
                Log.d(TAG, "started wizard dialog")
                return
            }
        } catch (e: PersistenceException) {
            throw RuntimeException(e)
        }

        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        val header = navigationView.getHeaderView(0)
        drawerHolder = DrawerNavigationViewHolder(header)

        setSupportActionBar(toolbar)
        mDrawerIndicatorTintFilter = LightingColorFilter(0, resources.getColor(R.color.tintDrawerIndicator))


        installConnectivityManager()
        addBackStackListener()

        setupAccount()
        initDrawer()
        handleIntent(intent)
    }

    private fun setupAccount() {
        AccountHelper.createSyncAccount(applicationContext)
        AccountHelper.configurePeriodicPolling()
    }

    fun installConnectivityManager(connectivityManager: ConnectivityManager) {
        this.connectivityManager = connectivityManager
        installConnectivityManager()
    }

    fun installConnectivityManager() {
        connectivityManager.setListener(object : ConnectivityManager.ConnectivityListener {

            private var offlineIndicator: AlertDialog? = null

            override fun handleConnectionLost(): Unit {
                if (offlineIndicator == null) {
                    val builder = AlertDialog.Builder(self)
                    builder.setTitle(R.string.no_connection).setIcon(R.drawable.information).setNegativeButton(R.string.close_app) { dialog, id -> self.finishAffinity() }.setPositiveButton(R.string.retry_action, null)
                    offlineIndicator = builder.create()
                    offlineIndicator?.let {
                        it.setCancelable(false)
                        it.show()
                        it.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                            if (connectivityManager.isConnected) {
                                offlineIndicator?.dismiss()
                                offlineIndicator = null
                            }
                        }
                    }
                } else {
                    offlineIndicator?.show()
                }
            }

            override fun handleConnectionEstablished() {
                offlineIndicator?.let {
                    if (it.isShowing) {
                        it.dismiss()
                        offlineIndicator = null
                    }
                }
            }

            override fun onDestroy() {
                offlineIndicator?.let { it.dismiss() }
            }
        })
    }

    fun handleMainFragmentChange() {
        // Set FAB visibility according to currently visible fragment
        val activeFragment = fragmentManager.findFragmentById(R.id.fragment_container)

        if (activeFragment is BaseFragment) {
            toolbar.title = activeFragment.title
            if (activeFragment.isFabNeeded) {
                fab.show()
            } else {
                fab.hide()
            }
            if (!activeFragment.supportSubtitle()) {
                toolbar.subtitle = null
            } else {
                activeFragment.updateSubtitle()
            }
        }
    }

    private fun addBackStackListener() {
        fragmentManager.addOnBackStackChangedListener {
            this@MainActivity.handleMainFragmentChange()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        TEST = intent.getBooleanExtra(TEST_RUN, false)

        // Checks if a fragment should be launched
        val startFilesFragment = intent.getBooleanExtra(START_FILES_FRAGMENT, true)
        val startContactsFragment = intent.getBooleanExtra(START_CONTACTS_FRAGMENT, false)
        val activeContact = intent.getStringExtra(ACTIVE_CONTACT)
        if (startContactsFragment) {
            navigator.selectContactsFragment()
            navigator.selectChatFragment(activeContact)
        } else if (startFilesFragment) {
            navigator.selectFilesFragment()
        }
    }

    @OnClick(R.id.fab)
    fun floatingActionButtonClick() {
        val activeFragment = fragmentManager.findFragmentById(R.id.fragment_container)
        val activeFragmentTag = activeFragment.tag
        if (activeFragment is BaseFragment) {
        //call fab action in basefragment. if fragment handled this, we are done
            if (activeFragment.handleFABAction()) {
                return
            }
        }
        when (activeFragmentTag) {
            MainNavigator.TAG_MANAGE_IDENTITIES_FRAGMENT -> selectAddIdentityFragment()

            else -> Log.e(TAG, "Unknown FAB action for fragment tag: " + activeFragmentTag)
        }
    }

    override fun onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            val activeFragment = fragmentManager.findFragmentById(R.id.fragment_container)
            if (activeFragment == null) {
                super.onBackPressed()
                return
            }
            if (activeFragment.tag == null) {
                fragmentManager.popBackStack()
            } else {
                when (activeFragment.tag) {
                    MainNavigator.TAG_CONTACT_LIST_FRAGMENT -> super.onBackPressed()
                    else -> if (fragmentManager.backStackEntryCount > 0) {
                        fragmentManager.popBackStack()
                    } else {
                        finishAffinity()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        // Inflate the menu; this adds items to the action bar if it is present.
        //  getMenuInflater().inflate(R.menu.ab_main, menu);
        return true
    }

    fun selectIdentity(identity: Identity) {
        changeActiveIdentity(identity)
    }

    fun addIdentity(identity: Identity) {

        try {
            identityRepository.save(identity)
        } catch (e: PersistenceException) {
            throw RuntimeException(e)
        }

        changeActiveIdentity(identity, null)
        Snackbar.make(appBarMain, "Added identity: " + identity.alias, Snackbar.LENGTH_LONG).show()
        navigator.selectFilesFragment()
    }

    private fun changeActiveIdentity(identity: Identity, intent: Intent? = null) {
        var changeIntent: Intent? = intent
        if (identity != activeIdentity) {
            appPreferences.lastActiveIdentityKey = identity.keyIdentifier
            if (changeIntent == null) {
                changeIntent = Intent(this, MainActivity::class.java)
            }
            changeIntent.putExtra(ACTIVE_IDENTITY, identity.keyIdentifier)
            changeIntent.putExtra(TEST_RUN, TEST)
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            startActivity(changeIntent)
        }
    }

    override fun deleteIdentity(identity: Identity) {
        try {
            identityRepository.delete(identity)
            if (identityRepository.findAll().identities.size == 0) {
                UIHelper.showDialogMessage(this, R.string.dialog_headline_info,
                        R.string.last_identity_delete_create_new) { dialog, which -> this@MainActivity.selectAddIdentityFragment() }
            } else {
                try {
                    changeActiveIdentity(identityRepository.findAll().identities.iterator().next())
                } catch (e: PersistenceException) {
                    throw RuntimeException(e)
                }

            }
        } catch (e: PersistenceException) {
            throw RuntimeException(e)
        }

    }

    override fun modifyIdentity(identity: Identity) {
        try {
            identityRepository.save(identity)
        } catch (e: PersistenceException) {
            throw RuntimeException(e)
        }

        drawerHolder.textViewSelectedIdentity.text = activeIdentity.alias
    }

    override fun onDestroy() {

        if (isTaskRoot) {
            CacheFileHelper().freeCacheAsynchron(ctx.applicationContext)
        }

        connectivityManager.onDestroy()

        unregisterReceiver(accountBroadCastReceiver)

        super.onDestroy()
    }

    fun selectIdentityLayoutClick() {

        if (identityMenuExpanded) {
            drawerHolder.imageViewExpandIdentity.setImageResource(R.drawable.menu_down)
            drawerHolder.imageViewExpandIdentity.colorFilter = mDrawerIndicatorTintFilter
            navigationView.menu.clear()
            navigationView.inflateMenu(R.menu.activity_main_drawer)
            identityMenuExpanded = false
        } else {
            drawerHolder.imageViewExpandIdentity.setImageResource(R.drawable.menu_up)
            drawerHolder.imageViewExpandIdentity.colorFilter = mDrawerIndicatorTintFilter
            navigationView.menu.clear()
            val identityList: List<Identity>
            try {
                identityList = ArrayList(
                        identityRepository.findAll().identities)
            } catch (e: PersistenceException) {
                Log.e(TAG, "Could not list identities", e)
                identityList = ArrayList<Identity>()
            }

            Collections.sort(identityList) { lhs, rhs -> lhs.alias.compareTo(rhs.alias) }
            for (identity in identityList) {
                navigationView.menu.add(NAV_GROUP_IDENTITIES, Menu.NONE, Menu.NONE, identity.alias).setIcon(R.drawable.account).setOnMenuItemClickListener {
                    drawer.closeDrawer(GravityCompat.START)
                    this@MainActivity.selectIdentity(identity)
                    true
                }
            }
            navigationView.menu.add(NAV_GROUP_IDENTITY_ACTIONS, Menu.NONE, Menu.NONE, R.string.add_identity).setIcon(R.drawable.plus_circle).setOnMenuItemClickListener {
                drawer.closeDrawer(GravityCompat.START)
                this@MainActivity.selectAddIdentityFragment()
                true
            }
            navigationView.menu.add(NAV_GROUP_IDENTITY_ACTIONS, Menu.NONE, Menu.NONE, R.string.manage_identities).setIcon(R.drawable.settings).setOnMenuItemClickListener {
                drawer.closeDrawer(GravityCompat.START)
                navigator.selectManageIdentitiesFragment()
                true
            }
            navigationView.menu.add(NAV_GROUP_IDENTITY_ACTIONS, Menu.NONE, Menu.NONE, R.string.logout).setIcon(R.drawable.account_off).setOnMenuItemClickListener {
                UIHelper.showConfirmationDialog(self, R.string.logout,
                        R.string.logout_confirmation, R.drawable.account_off
                ) { dialog, which -> accountManager.logout() }
                true
            }
            identityMenuExpanded = true
        }
    }

    private fun initDrawer() {
        toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        drawerHolder.qabelLogo.setOnClickListener {
            drawer.closeDrawer(GravityCompat.START)
            showQRCode(self, this@MainActivity.activeIdentity)
        }

        drawerHolder.selectIdentityLayout.setOnClickListener { this@MainActivity.selectIdentityLayoutClick() }

        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        toggle.isDrawerIndicatorEnabled = true
        navigationView.setNavigationItemSelectedListener(this)

        // Map QR-Code indent to alias textview in nav_header_main
        val boxName = AppPreference(self).accountName
        drawerHolder.textViewBoxAccountName.text = boxName ?: getString(R.string.app_name)

        drawerHolder.imageViewExpandIdentity.colorFilter = mDrawerIndicatorTintFilter

        drawerHolder.textViewSelectedIdentity.text = activeIdentity.alias
        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

            }

            override fun onDrawerOpened(drawerView: View) {

            }

            override fun onDrawerClosed(drawerView: View) {
                navigationView.menu.clear()
                navigationView.inflateMenu(R.menu.activity_main_drawer)
                drawerHolder.imageViewExpandIdentity.setImageResource(R.drawable.menu_down)
                identityMenuExpanded = false
            }

            override fun onDrawerStateChanged(newState: Int) {

            }
        })
    }

    private fun selectAddIdentityFragment() {

        val i = Intent(self, CreateIdentityActivity::class.java)
        val identitiesCount: Int
        try {
            identitiesCount = identityRepository.findAll().identities.size
        } catch (e: PersistenceException) {
            throw RuntimeException(e)
        }

        i.putExtra(CreateIdentityActivity.FIRST_RUN, identitiesCount == 0)

        if (identitiesCount == 0) {
            finish()
            self.startActivity(i)
        } else {
            self.startActivityForResult(i, REQUEST_CREATE_IDENTITY)
        }
    }

    override fun getComponent(): MainActivityComponent {
        return component
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.nav_tellafriend) {
            ShareHelper.tellAFriend(this)
        }
        if (id == R.id.nav_contacts) {
            navigator.selectContactsFragment()
        } else if (id == R.id.nav_browse) {
            navigator.selectFilesFragment()
        } else if (id == R.id.nav_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, REQUEST_SETTINGS)
        } else if (id == R.id.nav_about) {
            navigator.selectAboutFragment()
        } else if (id == R.id.nav_help) {
            navigator.selectHelpFragment()
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {

        private val REQUEST_SETTINGS = 17
        private val REQUEST_CREATE_IDENTITY = 16
        const val REQUEST_EXPORT_IDENTITY = 18
        const val REQUEST_EXTERN_VIEWER_APP = 19
        const val REQUEST_EXTERN_SHARE_APP = 20

        private val TAG = "BoxMainActivity"

        const val REQUEST_EXPORT_IDENTITY_AS_CONTACT = 19

        private val NAV_GROUP_IDENTITIES = 1
        private val NAV_GROUP_IDENTITY_ACTIONS = 2

        // Intent extra to specify if the files fragment should be started
        // Defaults to true and is used in tests to shortcut the activity creation

        const val START_FILES_FRAGMENT = "START_FILES_FRAGMENT"
        const val START_CONTACTS_FRAGMENT = "START_CONTACTS_FRAGMENT"
        const val ACTIVE_IDENTITY = "ACTIVE_IDENTITY"
        const val ACTIVE_CONTACT = "ACTIVE_CONTACT"
        const val START_FILES_FRAGMENT_PATH = "START_FILES_FRAGMENT_PATH"
        // Intent extra to specify that the activity is in test mode which disables auto-refresh
        const val TEST_RUN = "TEST_RUN"

        @JvmStatic
        fun showQRCode(activity: MainActivity, identity: Identity) {
            activity.fragmentManager.beginTransaction().replace(R.id.fragment_container, QRcodeFragment.newInstance(identity), null).addToBackStack(null).commit()
        }
    }
}
