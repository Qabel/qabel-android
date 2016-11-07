package de.qabel.qabelbox.base

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.View
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.model.*
import de.qabel.chat.repository.ChatDropMessageRepository
import de.qabel.core.config.Identity
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.exception.PersistenceException
import de.qabel.core.ui.initials
import de.qabel.qabelbox.QblBroadcastConstants.*
import de.qabel.qabelbox.R
import de.qabel.qabelbox.account.AccountManager
import de.qabel.qabelbox.account.AccountStatusCodes
import de.qabel.qabelbox.communication.connection.ConnectivityManager
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.contacts.extensions.color
import de.qabel.qabelbox.contacts.view.widgets.IdentityIconDrawable
import de.qabel.qabelbox.dagger.HasComponent
import de.qabel.qabelbox.dagger.components.ActiveIdentityComponent
import de.qabel.qabelbox.dagger.modules.ActivityModule
import de.qabel.qabelbox.dagger.modules.ActiveIdentityModule
import de.qabel.qabelbox.helper.AccountHelper
import de.qabel.qabelbox.helper.CacheFileHelper
import de.qabel.qabelbox.helper.Sanity
import de.qabel.qabelbox.helper.UIHelper
import de.qabel.qabelbox.index.AndroidIndexSyncService
import de.qabel.qabelbox.index.ContactSyncAdapter
import de.qabel.qabelbox.index.IndexIdentityListener
import de.qabel.qabelbox.index.preferences.IndexPreferences
import de.qabel.qabelbox.listeners.intentListener
import de.qabel.qabelbox.navigation.MainNavigator
import de.qabel.qabelbox.permissions.DataPermissionsAdapter
import de.qabel.qabelbox.permissions.hasContactsReadPermission
import de.qabel.qabelbox.permissions.isPermissionGranted
import de.qabel.qabelbox.permissions.requestContactsReadPermission
import de.qabel.qabelbox.settings.SettingsActivity
import de.qabel.qabelbox.startup.activities.CreateIdentityActivity
import de.qabel.qabelbox.sync.FirebaseTopicManager
import de.qabel.qabelbox.sync.TopicManager
import de.qabel.qabelbox.util.ShareHelper
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.browse
import org.jetbrains.anko.ctx
import org.jetbrains.anko.toast
import javax.inject.Inject

class MainActivity : CrashReportingActivity(),
        HasComponent<ActiveIdentityComponent>,
        TopicManager by FirebaseTopicManager(),
        AnkoLogger, DataPermissionsAdapter,
        ActiveIdentityActivity
{
    override val activeIdentityKey: String?
        get() = intent.getStringExtra(ACTIVE_IDENTITY)

    override val permissionContext: Context = this

    var TEST = false

    @BindView(R.id.fab)
    lateinit var fab: FloatingActionButton
    @BindView(R.id.toolbar)
    lateinit internal var toolbar: Toolbar
    @BindView(R.id.app_bap_main)
    lateinit var appBarMain: View

    @Inject
    lateinit internal var connectivityManager: ConnectivityManager

    @Inject
    lateinit var activeIdentity: Identity
        internal set

    @Inject
    lateinit internal var identityRepository: IdentityRepository
    @Inject
    lateinit var contactRepository: ContactRepository

    @Inject
    lateinit var messageRepository: ChatDropMessageRepository

    @Inject
    lateinit internal var appPreferences: AppPreference
    @Inject
    lateinit internal var indexPreferences: IndexPreferences

    @Inject
    lateinit internal var navigator: MainNavigator

    @Inject
    lateinit internal var accountManager: AccountManager

    @Inject
    lateinit var activityStartup: ActivityStartup

    lateinit private var component: ActiveIdentityComponent

    lateinit private var drawer: Drawer
    lateinit private var drawerAccountHeader: AccountHeader
    lateinit private var contacts: PrimaryDrawerItem
    lateinit private var files: PrimaryDrawerItem
    lateinit private var chats: PrimaryDrawerItem
    lateinit var toggle: ActionBarDrawerToggle

    private var lastBackPressTime = 0L
    private val backPressConfirmInterval = 2000L

    private val profileIconSize: Int
        get() = ctx.resources.getDimension(
                R.dimen.material_drawer_item_profile_icon_width).toInt()


    private val identityChangedListener = intentListener(Identities.IDENTITY_CHANGED) {
        val changedIdentity = it.getSerializableExtra(Identities.KEY_IDENTITY) as Identity
        if (changedIdentity.id == activeIdentity.id) {
            activeIdentity = changedIdentity
            drawerAccountHeader.activeProfile.email.text = activeIdentity.email
            drawerAccountHeader.activeProfile.name.text = activeIdentity.alias
            drawerAccountHeader.updateProfile(drawerAccountHeader.activeProfile)
        } else {
            drawerAccountHeader.profiles.find {
                it.identifier == changedIdentity.id.toLong()
            }?.apply {
                val size = profileIconSize
                withName(changedIdentity.alias)
                withIcon(IdentityIconDrawable(
                        width = size,
                        height = size,
                        text = changedIdentity.initials(),
                        color = changedIdentity.color(ctx)))
                drawerAccountHeader.updateProfile(this)
            }
        }
    }
    private val identityRemovedListener = intentListener(Identities.IDENTITY_REMOVED) {
        val deletedIdentity = it.getSerializableExtra(Identities.KEY_IDENTITY) as Identity
        val identities = identityRepository.findAll()
        deletedIdentity.dropUrls.forEach {
            unSubscribe(it)
        }
        if (identities.identities.size == 0) {
            UIHelper.showDialogMessage(this, R.string.dialog_headline_info,
                    R.string.last_identity_delete_create_new) { dialog, which ->
                this@MainActivity.selectAddIdentityFragment()
                this@MainActivity.finish()
            }
        } else if (activeIdentity == deletedIdentity) {
            changeActiveIdentity(identities.identities.first())
        } else {
            drawerAccountHeader.profiles.find { it.identifier == deletedIdentity.id.toLong() }?.let {
                drawerAccountHeader.removeProfile(it)
            }
        }
    }

    override val intentListeners = listOf(
            intentListener(Account.ACCOUNT_CHANGED, {
                val statusCode = it.getIntExtra(STATUS_CODE_PARAM, -1)
                when (statusCode) {
                    AccountStatusCodes.LOGOUT -> finish()
                }
            }),
            intentListener(Chat.MESSAGE_STATE_CHANGED, {
                updateNewMessageBadge()
            }),
            identityChangedListener,
            identityRemovedListener)

    val indexIdentityListener = IndexIdentityListener()

    private fun updateNewMessageBadge() {
        val size = try {
            messageRepository.findNew(activeIdentity.id).size
        } catch (e: PersistenceException) {
            0
        }

        if (size == 0) {
            drawer.updateBadge(chats.identifier, null)
        } else {
            chats.withBadge(size.toString())
            drawer.updateItem(chats)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = applicationComponent.plus(ActivityModule(this)).plus(ActiveIdentityModule(this))
        component.inject(this)
        Log.d(TAG, "onCreate " + this.hashCode())

        registerReceiver(indexIdentityListener, indexIdentityListener.createIntentFilter())

        if (!activityStartup.onCreate()) {
            return
        }

        if (!indexPreferences.contactSyncAsked ||
                (indexPreferences.contactSyncEnabled && !hasContactsReadPermission())) {
            requestContactsPermission()
        }


        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        setSupportActionBar(toolbar)

        installConnectivityManager()

        setupAccount()
        initDrawer()
        identityRepository.findAll().identities.flatMap { it.dropUrls }.forEach {
            info("Subscribing to drop id $it")
            subscribe(it)
        }
        handleIntent(intent)

        AndroidIndexSyncService.startSyncVerifications(this)
    }

    fun requestContactsPermission() {
        requestContactsReadPermission(this, REQUEST_CONTACTS_READ_PERMISSION) {
            indexPreferences.contactSyncEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        updateNewMessageBadge()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CONTACTS_READ_PERMISSION) {
            isPermissionGranted(Manifest.permission.READ_CONTACTS, permissions, grantResults, {
                indexPreferences.contactSyncEnabled = true
                ContactSyncAdapter.Manager.configureSync(applicationContext)
                ContactSyncAdapter.Manager.startOnDemandSyncAdapter()
            }, {
                indexPreferences.contactSyncEnabled = false
            })
        }
    }

    private fun setupAccount() {
        AccountHelper.createSyncAccount(applicationContext)
        AccountHelper.configurePeriodicPolling()
        AccountHelper.startOnDemandSyncAdapter()
        ContactSyncAdapter.Manager.configureSync(applicationContext)
    }

    fun installConnectivityManager(connectivityManager: ConnectivityManager) {
        this.connectivityManager = connectivityManager
        installConnectivityManager()
    }

    fun installConnectivityManager() {
        connectivityManager.listener = (object : ConnectivityManager.ConnectivityListener {

            private var offlineIndicator: AlertDialog? = null

            override fun handleConnectionLost(): Unit {
                runOnUiThread {
                    if (offlineIndicator == null) {
                        val builder = AlertDialog.Builder(this@MainActivity)
                        builder.setTitle(R.string.no_connection).setIcon(R.drawable.information).setNegativeButton(R.string.close_app) {
                            dialog, id ->
                            finishAffinity()
                        }.setPositiveButton(R.string.retry_action, null)
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
            }

            override fun handleConnectionEstablished() {
                runOnUiThread {
                    offlineIndicator?.let {
                        if (it.isShowing) {
                            it.dismiss()
                            offlineIndicator = null
                        }
                    }
                }
            }

            override fun onDestroy() {
                runOnUiThread {
                    offlineIndicator?.let { it.dismiss() }
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(ACTIVE_IDENTITY)) {
            val identityKey = intent.getStringExtra(ACTIVE_IDENTITY)
            if (identityKey != activeIdentity.keyIdentifier) {
                val identity = identityRepository.find(identityKey)
                changeActiveIdentity(identity, intent)
            }
        }

        TEST = intent.getBooleanExtra(TEST_RUN, false)

        // Checks if a fragment should be launched
        val startFilesFragment = intent.getBooleanExtra(START_FILES_FRAGMENT, false)
        val startChatFragment = intent.getBooleanExtra(START_CHAT_FRAGMENT, false)
        val activeContact = intent.getStringExtra(ACTIVE_CONTACT)

        if (startChatFragment) {
            drawer.setSelection(chats)
            navigator.selectChatOverviewFragment()
            activeContact?.apply { navigator.selectChatFragment(this) }
        } else if (startFilesFragment) {
            drawer.setSelection(files)
            navigator.selectFilesFragment()
        } else {
            drawer.setSelection(chats)
            navigator.selectChatOverviewFragment()
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
        if (drawer.isDrawerOpen) {
            drawer.closeDrawer()
            return
        }
        val activeFragment = fragmentManager.findFragmentById(R.id.fragment_container)
        if (activeFragment == null) {
            super.onBackPressed()
            return
        }
        if (activeFragment is BaseFragment && activeFragment.onBackPressed()) {
            return
        }
        if (activeFragment.tag == null) {
            fragmentManager.popBackStack()
            return
        }
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
            return
        }
        if (backPressConfirmed()) {
            finishAffinity()
            return
        }
        toast(R.string.confirm_exit)
    }

    private fun backPressConfirmed(): Boolean {
        val now = System.currentTimeMillis()
        lastBackPressTime.let {
            lastBackPressTime = now
            return it + backPressConfirmInterval > now
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        // Inflate the menu; this adds items to the action bar if it is present.
        //  getMenuInflater().inflate(R.menu.ab_main, menu);
        return true
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

    override fun onDestroy() {

        if (isTaskRoot) {
            CacheFileHelper().freeCacheAsynchron(ctx.applicationContext)
        }

        connectivityManager.onDestroy()
        unregisterReceiver(indexIdentityListener)
        super.onDestroy()
    }


    private fun initDrawer() {
        contacts = PrimaryDrawerItem().apply {
            withIdentifier(R.id.nav_contacts.toLong())
            withName(R.string.Contacts)
            withIcon(R.drawable.account_multiple)
        }
        files = PrimaryDrawerItem().apply {
            withIdentifier(R.id.nav_browse.toLong())
            withName(R.string.filebrowser)
            withIcon(R.drawable.folder)
        }
        chats = PrimaryDrawerItem().apply {
            withIdentifier(R.id.nav_chats.toLong())
            withName(R.string.conversations)
            withIcon(R.drawable.message_text)
            withBadgeStyle(BadgeStyle().withTextColorRes(R.color.colorAccent).withColorRes(R.color.md_dark_background))
        }
        val settings = SecondaryDrawerItem().apply {
            withIdentifier(R.id.nav_settings.toLong())
            withName(R.string.action_settings)
            withIcon(R.drawable.settings)
            withSelectable(false)
        }
        val upgrade = SecondaryDrawerItem().apply {
            withIdentifier(R.id.nav_upgrade.toLong())
            withName(R.string.upgrade)
            withIcon(R.drawable.plus_circle)
            withSelectable(false)
        }
        val tellAFriend = SecondaryDrawerItem().apply {
            withIdentifier(R.id.nav_tellafriend.toLong())
            withName(R.string.action_tellafriend)
            withIcon(R.drawable.heart)
            withSelectable(false)
        }
        val about = SecondaryDrawerItem().apply {
            withIdentifier(R.id.nav_about.toLong())
            withName(R.string.action_about)
            withIcon(R.drawable.information)
        }
        val help = SecondaryDrawerItem().apply {
            withIdentifier(R.id.nav_help.toLong())
            withName(R.string.help)
            withIcon(R.drawable.help_circle)
        }
        drawer = with(DrawerBuilder()) {
            withActivity(this@MainActivity)
            withToolbar(toolbar)
            addDrawerItems(
                    contacts,
                    chats,
                    files
            )
            addDrawerItems(
                    DividerDrawerItem(),
                    settings,
                    upgrade,
                    tellAFriend,
                    about,
                    help)
            withOnDrawerItemClickListener { view, i, iDrawerItem ->
                when (iDrawerItem) {
                    contacts -> navigator.selectContactsFragment()
                    files -> navigator.selectFilesFragment()
                    chats -> navigator.selectChatOverviewFragment()
                    settings -> {
                        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                        startActivityForResult(intent, REQUEST_SETTINGS)
                    }
                    upgrade -> browse(getString(R.string.upgradeHomepage))
                    tellAFriend -> ShareHelper.tellAFriend(this@MainActivity)
                    about -> navigator.selectAboutFragment()
                    help -> navigator.selectHelpFragment()
                    else -> {
                        return@withOnDrawerItemClickListener false
                    }
                }
                drawer.closeDrawer()
                true
            }
            drawerAccountHeader = buildAccountHeader()
            withAccountHeader(drawerAccountHeader)
            withOnDrawerNavigationListener {
                if (drawer.isDrawerOpen) {
                    drawer.closeDrawer()
                } else {
                    if (toggle.isDrawerIndicatorEnabled) {
                        drawer.openDrawer()
                    } else {
                        onBackPressed()
                    }
                }
                true
            }
            build()
        }
        toggle = drawer.actionBarDrawerToggle
    }

    private fun buildAccountHeader(): AccountHeader {
        val size = profileIconSize
        val identityIcon = { identity: Identity ->
            IdentityIconDrawable(
                    width = size,
                    height = size,
                    text = identity.initials(),
                    color = identity.color(ctx))
        }
        val profileMap = identityRepository.findAll().identities.filterNot {
            it.keyIdentifier == activeIdentity.keyIdentifier
        }.mapIndexed { i, identity ->
            Pair(ProfileDrawerItem().apply {
                withIdentifier(identity.id.toLong())
                withName(identity.alias)
                withEmail(identity.email)
                withIcon(identityIcon(identity))
                withNameShown(true)
            }, identity)
        }.toMap()
        val activeIdentityItem = ProfileDrawerItem().apply {
            withIdentifier(activeIdentity.id.toLong())
            withName(activeIdentity.alias)
            withIcon(identityIcon(activeIdentity))
            withEmail(activeIdentity.email)
            withNameShown(true)
        }
        val addIdentity = ProfileSettingDrawerItem().apply {
            withName(ctx.getString(R.string.add_identity))
            withIcon(R.drawable.plus_circle)
            withSelectable(false)
        }
        val manageIdentities = ProfileSettingDrawerItem().apply {
            withName(ctx.getString(R.string.manage_identities))
            withIcon(R.drawable.settings)
            withSelectable(false)
        }
        val accountHeader = with(AccountHeaderBuilder()) {
            withActivity(this@MainActivity)
            withHeaderBackground(R.drawable.drawer_v7)
            addProfiles(activeIdentityItem)
            addProfiles(*profileMap.keys.sortedBy { it.name.toString() }.toTypedArray())
            addProfiles(addIdentity)
            addProfiles(manageIdentities)
            withOnAccountHeaderListener { view, iProfile, current ->
                when (iProfile) {
                    activeIdentityItem -> navigator.selectQrCodeFragment(activeIdentity.toContact())
                    addIdentity -> selectAddIdentityFragment()
                    manageIdentities -> navigator.selectManageIdentitiesFragment()
                    else -> {
                        if (!current) {
                            profileMap[iProfile]?.let {
                                changeActiveIdentity(it)
                            }
                        }
                    }
                }
                drawer.closeDrawer()
                true
            }
            build()
        }
        return accountHeader.apply { isSelectionListShown }
    }

    private fun selectAddIdentityFragment() {
        val i = Intent(this, CreateIdentityActivity::class.java)
        startActivity(i)
    }

    override fun getComponent(): ActiveIdentityComponent {
        return component
    }

    companion object {

        private val REQUEST_SETTINGS = 17
        const val REQUEST_EXPORT_IDENTITY = 18
        const val REQUEST_EXTERN_VIEWER_APP = 19
        const val REQUEST_EXTERN_SHARE_APP = 20

        private val TAG = "BoxMainActivity"

        const val REQUEST_EXPORT_IDENTITY_AS_CONTACT = 21

        const val REQUEST_CONTACTS_READ_PERMISSION = 22


        // Intent extra to specify if the files fragment should be started
        // Defaults to true and is used in tests to shortcut the activity creation

        const val START_FILES_FRAGMENT = "START_FILES_FRAGMENT"
        const val START_CHAT_FRAGMENT = "START_CHAT_FRAGMENT"
        const val ACTIVE_CONTACT = "ACTIVE_CONTACT"
        const val START_FILES_FRAGMENT_PATH = "START_FILES_FRAGMENT_PATH"
        // Intent extra to specify that the activity is in test mode which disables auto-refresh
        const val TEST_RUN = "TEST_RUN"

    }
}
