package de.qabel.qabelbox.startup.activities

import android.Manifest.permission.READ_PHONE_STATE
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.config.factory.DropUrlGenerator
import de.qabel.core.index.formatPhoneNumber
import de.qabel.core.index.formatPhoneNumberReadable
import de.qabel.core.index.isValidPhoneNumber
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.R
import de.qabel.qabelbox.TestConstants
import de.qabel.qabelbox.base.ACTIVE_IDENTITY
import de.qabel.qabelbox.base.MainActivity
import de.qabel.qabelbox.communication.PrefixServer
import de.qabel.qabelbox.communication.callbacks.JsonRequestCallback
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.identity.interactor.IdentityInteractor
import de.qabel.qabelbox.index.IndexIdentityListener
import de.qabel.qabelbox.index.preferences.IndexPreferences
import de.qabel.qabelbox.permissions.DataPermissionsAdapter
import de.qabel.qabelbox.permissions.hasPhonePermission
import de.qabel.qabelbox.permissions.isPermissionGranted
import de.qabel.qabelbox.permissions.requestPhonePermission
import de.qabel.qabelbox.startup.fragments.BaseIdentityFragment
import de.qabel.qabelbox.startup.fragments.CreateIdentityEditTextFragment
import de.qabel.qabelbox.startup.fragments.CreateIdentityFinalFragment
import de.qabel.qabelbox.startup.fragments.CreateIdentityMainFragment
import okhttp3.Response
import org.jetbrains.anko.ctx
import org.jetbrains.anko.toast
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

class CreateIdentityActivity : BaseWizardActivity(), QabelLog, DataPermissionsAdapter {

    override val permissionContext: Context by lazy { ctx }

    private val TAG = this.javaClass.simpleName
    private val REQUEST_READ_PHONE_STATE = 1

    private var identityName: String = ""
    private var phoneNumber: String = ""
    private var email: String = ""

    var createdIdentity: Identity? = null
    private var prefix: String? = null
    internal var tryCount = 0

    @Inject
    lateinit internal var identityInteractor: IdentityInteractor
    @Inject
    lateinit internal var indexPreferences: IndexPreferences

    val indexIdentityListener = IndexIdentityListener()

    internal val dropUrlGenerator: DropUrlGenerator by lazy { DropUrlGenerator(getString(R.string.dropServer)) }

    private var existingIdentities: Identities? = null

    override val headerFragmentText: String get() = identityName
    override val headerSecondLine: String get() = email
    override val headerThirdLine: String
        get() = if (phoneNumber.isNotBlank()) formatPhoneNumberReadable(phoneNumber) else phoneNumber

    override val actionBarTitle: Int = R.string.headline_add_identity
    override val wizardEntityLabel: String by lazy { getString(R.string.identity) }

    val enterPhoneFragment: CreateIdentityEditTextFragment = CreateIdentityEditTextFragment.newInstance(
            R.string.create_identity_enter_phone,
            R.string.phone_number,
            object : NextChecker {
                override fun check(view: View): String? {
                    val phone = (view as EditText).text.toString().trim()
                    if (!phone.isEmpty()) {
                        if (isValidPhoneNumber(phone)) {
                            phoneNumber = formatPhoneNumber(phone)
                        } else {
                            return getString(R.string.phone_number_invalid)
                        }
                    }
                    //Last input
                    createIdentity()
                    return null
                }
            }, InputType.TYPE_CLASS_PHONE, true)

    val enterAliasFragment: CreateIdentityEditTextFragment = CreateIdentityEditTextFragment.newInstance(
            R.string.create_identity_enter_name,
            R.string.create_identity_enter_name_hint,
            object : NextChecker {
                override fun check(view: View): String? {
                    val alias = (view as EditText).text.toString().trim()
                    if (alias.isEmpty()) {
                        return getString(R.string.create_identity_enter_all_data)
                    }
                    existingIdentities?.let {
                        if (it.entities.any { it.alias == alias }) {
                            return getString(R.string.create_identity_already_exists)
                        }
                    }
                    identityName = alias
                    return null
                }
            })
    val enterMailFragment: CreateIdentityEditTextFragment = CreateIdentityEditTextFragment.newInstance(
            R.string.create_identity_enter_email,
            R.string.email_hint,
            object : NextChecker {
                override fun check(view: View): String? {
                    val inputEmail = (view as EditText).text.toString().trim()
                    val emailCheck = checkEMailAddress(inputEmail)
                    if (!inputEmail.isEmpty() && emailCheck != null) {
                        return emailCheck
                    }
                    email = inputEmail
                    return null
                }
            }, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, true)

    override fun onCreate(savedInstanceState: Bundle?) {
        QabelBoxApplication.getApplicationComponent(applicationContext).inject(this)
        super.onCreate(savedInstanceState)
        registerReceiver(indexIdentityListener, indexIdentityListener.createIntentFilter())
        identityInteractor.getIdentities().subscribe({
            existingIdentities = it
            canExit = it.identities.size > 0
        }, {
            existingIdentities = Identities()
        })
    }

    override fun onDestroy() {
        unregisterReceiver(indexIdentityListener)
        super.onDestroy()
    }

    private fun tryReadPhoneNumber() {
        if (!phoneNumber.isNullOrBlank()) return
        else if (indexPreferences.phoneStatePermission && !hasPhonePermission()) {
            requestPhonePermission(this, REQUEST_READ_PHONE_STATE, { indexPreferences.phoneStatePermission = false })
            return
        } else if (!indexPreferences.phoneStatePermission) return

        val phoneManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var phone = phoneManager.line1Number
        if (!phone.isNullOrBlank() && isValidPhoneNumber(phone)) {
            try {
                phone = formatPhoneNumberReadable(phone)
                enterPhoneFragment.setValue(phone)
                info("Phone number detected $phone")
                return
            } catch (ex: NumberFormatException) {
                error("Error parsing received system phone number $phone", ex)
            }
        }
        toast(R.string.cannot_read_phone_number)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_PHONE_STATE) {
            isPermissionGranted(READ_PHONE_STATE, permissions, grantResults, {
                indexPreferences.phoneStatePermission = true
                tryReadPhoneNumber()
            }, {
                indexPreferences.phoneStatePermission = false
            })
        }
    }

    override fun handleNextClick() {
        super.handleNextClick()
        if (step > 0 && tryCount != 3 && prefix == null) {
            loadPrefixInBackground()
        }
    }

    override fun onShowNext(fragment: BaseIdentityFragment) {
        when (fragment) {
            enterPhoneFragment -> tryReadPhoneNumber()
            enterMailFragment -> {
                //Account email as default for first identity
                if (email.isNullOrBlank() && existingIdentities?.identities?.size == 0) {
                    enterMailFragment.setValue(AppPreference(ctx).accountEMail)
                }
            }
        }
    }

    override val fragments: Array<BaseIdentityFragment> =
            arrayOf(CreateIdentityMainFragment(),
                    enterAliasFragment,
                    enterMailFragment,
                    enterPhoneFragment,
                    CreateIdentityFinalFragment())

    private fun createIdentity() {
        if (prefix != null) {
            identityInteractor.createIdentity(identityName, dropUrlGenerator.generateUrl(),
                    prefix!!, email, phoneNumber)
                    .subscribe({
                        info("Identity created ${it.alias} (${it.keyIdentifier})")
                        createdIdentity = it
                    }, {
                        error("Failed to create identity", it)
                    })
        } else {
            loadPrefixInBackground()
            toast(getString(R.string.create_idenity_cant_get_prefix))
        }
    }

    override fun completeWizard() {
        createdIdentity?.let {
            finish()
            val intent = Intent(ctx, MainActivity::class.java)
            intent.putExtra(ACTIVE_IDENTITY, it.keyIdentifier)
            intent.flags = Intent.FLAG_ACTIVITY_TASK_ON_HOME or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } ?: createIdentity()
    }

    private fun loadPrefixInBackground() {
        if (FAKE_COMMUNICATION) {
            prefix = TestConstants.PREFIX
            return
        }

        if (tryCount < 3) {
            val prefixServer = PrefixServer(applicationContext)
            prefixServer.getPrefix(this, object : JsonRequestCallback(intArrayOf(201)) {

                override fun onError(e: Exception, response: Response?) {
                    Log.d(TAG, "Server communication failed: ", e)
                    tryCount++
                    loadPrefixInBackground()
                }

                override fun onJSONSuccess(response: Response, result: JSONObject) {
                    try {
                        Log.d(TAG, "Server response code: " + response.code())
                        prefix = result.getString("prefix")
                    } catch (e: JSONException) {
                        Log.e(TAG, "Cannot parse prefix from server", e)
                        tryCount++
                        loadPrefixInBackground()
                    }
                }
            })
        }
    }

    companion object {

        const val REQUEST_CODE_IMPORT_IDENTITY = 1
        /**
         * Fake the prefix request
         *
         *
         * This is set by the QblJUnitTestRunner to prevent network requests to the block server
         * in test runs.
         */
        var FAKE_COMMUNICATION = false
    }
}
