package de.qabel.qabelbox.ui

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.intent.rule.IntentsTestRule

import org.junit.After
import org.junit.Before
import org.junit.Rule

import de.qabel.core.config.Identity
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.TestConstants
import de.qabel.qabelbox.base.MainActivity
import de.qabel.qabelbox.communication.URLs
import de.qabel.qabelbox.ui.helper.SystemAnimations
import de.qabel.qabelbox.ui.helper.UIActionHelper
import de.qabel.qabelbox.ui.helper.UIBoxHelper

open class AbstractUITest : UITest {
    @JvmField
    @Rule
    var mActivityTestRule: IntentsTestRule<MainActivity> = MainActivityWithoutFilesFragmentTestRule(false)
    protected lateinit var mActivity: MainActivity
    protected lateinit var mBoxHelper: UIBoxHelper
    protected lateinit var identity: Identity
    private var wakeLock: PowerManager.WakeLock? = null
    private var mSystemAnimations: SystemAnimations? = null
    protected lateinit var mContext: Context
    protected lateinit var identityRepository: IdentityRepository
    protected lateinit var contactRepository: ContactRepository

    @After
    fun cleanUp() {
        wakeLock?.release()
        mSystemAnimations?.enableAll()
    }

    @Before
    @Throws(Throwable::class)
    open fun setUp() {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL)
        mContext = InstrumentationRegistry.getTargetContext()

        mBoxHelper = UIBoxHelper(mContext)
        mBoxHelper.createTokenIfNeeded(false)
        mBoxHelper.removeAllIdentities()
        identity = mBoxHelper.addIdentity("spoon123")
        identityRepository = mBoxHelper.identityRepository
        contactRepository = mBoxHelper.contactRepository

    }

    protected open val defaultIntent: Intent
        get() {
            val intent = Intent(mContext, MainActivity::class.java)
            intent.putExtra(MainActivity.START_FILES_FRAGMENT, false)
            intent.putExtra(MainActivity.TEST_RUN, true)
            return intent
        }

    protected fun launchActivity(intent: Intent?) {
        var intent = intent
        if (intent == null) {
            intent = defaultIntent
        }
        if (!intent.hasExtra(MainActivity.ACTIVE_IDENTITY)) {
            intent.putExtra(MainActivity.ACTIVE_IDENTITY, identity.keyIdentifier)
        }
        if (!intent.hasExtra(MainActivity.TEST_RUN)) {
            intent.putExtra(MainActivity.TEST_RUN, true)
        }
        mActivity = mActivityTestRule.launchActivity(intent)
        wakeLock = UIActionHelper.wakeupDevice(mActivity)
        mSystemAnimations = SystemAnimations(mActivity)
        mSystemAnimations?.disableAll()
    }
}
