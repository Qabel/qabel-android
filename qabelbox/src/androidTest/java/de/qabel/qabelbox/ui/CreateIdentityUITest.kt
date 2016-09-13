package de.qabel.qabelbox.ui

import android.os.Build
import android.os.PowerManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.rule.ActivityTestRule
import de.qabel.qabelbox.R
import de.qabel.qabelbox.R.string
import de.qabel.qabelbox.TestConstants
import de.qabel.qabelbox.activities.CreateIdentityActivity
import de.qabel.qabelbox.communication.URLs
import de.qabel.qabelbox.ui.action.QabelViewAction.setText
import de.qabel.qabelbox.ui.helper.SystemAnimations
import de.qabel.qabelbox.ui.helper.UIActionHelper
import de.qabel.qabelbox.ui.helper.UIBoxHelper
import de.qabel.qabelbox.ui.helper.UITestHelper
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreateIdentityUITest : UITest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(CreateIdentityActivity::class.java, false, false)

    private lateinit var helper: UIBoxHelper
    private var mActivity: CreateIdentityActivity? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var mSystemAnimations: SystemAnimations? = null

    @After
    fun cleanUp() {
        wakeLock?.release()
        mSystemAnimations?.enableAll()
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        helper = UIBoxHelper(InstrumentationRegistry.getTargetContext())
        helper.setTestAccount()
        helper.removeAllIdentities()

        URLs.setBaseBlockURL(TestConstants.BLOCK_URL)
        URLs.setBaseAccountingURL(TestConstants.ACCOUNTING_URL)

        mActivity = mActivityTestRule.launchActivity(null)
        wakeLock = UIActionHelper.wakeupDevice(mActivity)
        mSystemAnimations = SystemAnimations(mActivity)
        mSystemAnimations?.disableAll()

        //TODO TMP Fix option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant " + InstrumentationRegistry.getTargetContext().packageName
                            + " android.permission.READ_PHONE_STATE")
        }
    }

    @Test
    fun testCreateIdentity() {
        val name = "spoon2"
        val phone = "+49 234567890"
        val phoneFormatted = "+49 234 567890"
        val mail = "mail@test.de"
        createIdentityPerformEnterName(name)
        createIdentityEnterEmail(mail)
        createIdentityEnterPhone(phone)
        createIdentityPerformConfirm(name, mail, phoneFormatted)
    }

    @Test
    fun testCreateIdentityOptionals() {
        val name = "spoon2"
        val mail = "mail@test.de"
        createIdentityPerformEnterName(name)
        createIdentityEnterEmail(mail)
        //Ignore phone number
        performClickText(string.next)
        createIdentityPerformConfirm(name, mail, "")
    }

    @Test
    fun testCreateIdentityPreSetOptionals() {
        val name = "spoon2"
        val phone = "+49 234 567890"
        createIdentityPerformEnterName(name)
        //Inject value
        mActivity!!.apply {
            enterPhoneFragment.setValue(phone)
        }
        //Ignore email
        performClickText(string.next)
        onViewVisibleText(phone)
        performClickText(string.next)
        createIdentityPerformConfirm(name, "", phone)
    }

    private fun createIdentityPerformEnterName(name: String) {
        onViewVisibleText(string.create_identity_create).perform(click())
        onViewVisibleText(string.create_identity_enter_name)
        UITestHelper.screenShot(UITestHelper.getCurrentActivity(mActivity), "input")
        enterText(R.id.edit_text, name)
        performClickText(string.next)
    }

    private fun createIdentityEnterEmail(mail: String) {
        onViewVisibleText(string.create_identity_enter_email)
        UITestHelper.screenShot(UITestHelper.getCurrentActivity(mActivity), "input")
        //ImeAction Press -> closeKeyboard + next-click
        onView(withId(R.id.edit_text)).perform(setText(mail), pressImeActionButton())
    }

    private fun createIdentityEnterPhone(phone: String) {
        onViewVisibleText(string.create_identity_enter_phone)
        UITestHelper.screenShot(UITestHelper.getCurrentActivity(mActivity), "input")
        enterText(R.id.edit_text, phone)
        performClickText(string.next)
    }

    private fun createIdentityPerformConfirm(name: String, mail: String, phone: String) {
        onViewVisibleText(string.create_identity_successful)
        onViewVisibleText(string.create_identity_final_text)

        onViewVisibleText(name)
        if (!mail.isNullOrBlank()) {
            onViewVisibleText(mail)
        }
        if (!phone.isNullOrBlank()) {
            onViewVisibleText(phone)
        }

        performClickText(string.finish)
    }
}

