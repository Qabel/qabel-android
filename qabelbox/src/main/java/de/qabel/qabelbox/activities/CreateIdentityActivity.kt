package de.qabel.qabelbox.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.config.factory.DropUrlGenerator
import de.qabel.core.util.QblLogger
import de.qabel.core.util.error
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.R
import de.qabel.qabelbox.TestConstants
import de.qabel.qabelbox.communication.PrefixServer
import de.qabel.qabelbox.communication.callbacks.JsonRequestCallback
import de.qabel.qabelbox.fragments.BaseIdentityFragment
import de.qabel.qabelbox.fragments.CreateIdentityEditTextFragment
import de.qabel.qabelbox.fragments.CreateIdentityFinalFragment
import de.qabel.qabelbox.fragments.CreateIdentityMainFragment
import de.qabel.qabelbox.identity.interactor.IdentityUseCase
import okhttp3.Response
import org.jetbrains.anko.ctx
import org.jetbrains.anko.toast
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

class CreateIdentityActivity : BaseWizardActivity(), QblLogger {

    private val TAG = this.javaClass.simpleName

    private var identityName: String = ""
    private var phoneNumber: String = ""
    private var email: String = ""

    private var createdIdentity: Identity? = null
    private var prefix: String? = null
    internal var tryCount = 0

    @Inject
    lateinit internal var identityUseCase: IdentityUseCase
    lateinit internal var dropUrlGenerator: DropUrlGenerator

    private var existingIdentities: Identities? = null

    override val headerFragmentText: String get() = identityName

    override val actionBarTitle: Int = R.string.headline_add_identity

    override val wizardEntityLabel: String get() = getString(R.string.identity)

    override fun onCreate(savedInstanceState: Bundle?) {
        QabelBoxApplication.getApplicationComponent(applicationContext).inject(this)
        super.onCreate(savedInstanceState)
        dropUrlGenerator = DropUrlGenerator(getString(R.string.dropServer))
        identityUseCase.getIdentities().subscribe({
            existingIdentities = it
            canExit = it.identities.size > 0
        }, {
            existingIdentities = Identities()
        })
        println("CReate")
        tryReadPhoneNumber()
    }

    private fun tryReadPhoneNumber() {
        try {
            println("GET PHONE")
            val phoneManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val phone = phoneManager.line1Number
            if (phone != null && !phone.isEmpty()) {
                phoneNumber = phone
            }
            println("PHONE: " + phoneNumber)
        } catch(ex: Throwable) {
            ex.printStackTrace()
        }
    }


    override fun handleNextClick() {
        super.handleNextClick()
        if (step > 0 && tryCount != 3 && prefix == null) {
            loadPrefixInBackground()
        }
    }

    override val fragments: Array<BaseIdentityFragment> =
            arrayOf(CreateIdentityMainFragment(),
                    CreateIdentityEditTextFragment.newInstance(
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
                            }),
                    CreateIdentityEditTextFragment.newInstance(
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
                            }, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, true),
                    CreateIdentityEditTextFragment.newInstance(
                            R.string.create_identity_enter_phone,
                            R.string.phone_number,
                            object : NextChecker {
                                override fun check(view: View): String? {
                                    var phone = (view as EditText).text.toString().trim()
                                    if (!phone.isEmpty()) {
                                        if (!PhoneNumberUtils.isGlobalPhoneNumber(phone)) {
                                            /**
                                             * TODO deprecated or API 21...
                                             */
                                            phone = PhoneNumberUtils.formatNumber(phone)
                                        }
                                    }
                                    phoneNumber = phone
                                    createIdentity()
                                    return null
                                }
                            }, InputType.TYPE_CLASS_PHONE, true),
                    CreateIdentityFinalFragment())

    private fun createIdentity() {
        println("create")
        if (prefix != null) {
            identityUseCase.createIdentity(identityName, dropUrlGenerator.generateUrl(),
                    prefix!!, email, phoneNumber)
                    .subscribe({
                        println("Identity created ${it.alias} (${it.keyIdentifier})")
                        createdIdentity = it
                    }, {
                        println("Error create")
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
            intent.putExtra(MainActivity.ACTIVE_IDENTITY, it.keyIdentifier)
            intent.flags = Intent.FLAG_ACTIVITY_TASK_ON_HOME or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } ?: createIdentity()
    }

    fun setCreatedIdentity(identity: Identity) {
        createdIdentity = identity
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
