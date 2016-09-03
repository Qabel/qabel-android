package de.qabel.qabelbox.activities

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import de.qabel.qabelbox.R
import de.qabel.qabelbox.fragments.BaseIdentityFragment
import de.qabel.qabelbox.fragments.CreateIdentityHeaderFragment
import de.qabel.qabelbox.helper.Formatter
import de.qabel.qabelbox.helper.UIHelper
import kotlinx.android.synthetic.main.activity_create_identity.*

abstract class BaseWizardActivity : CrashReportingActivity() {

    protected abstract val fragments: Array<BaseIdentityFragment>

    protected abstract val actionBarTitle: Int
    protected abstract val wizardEntityLabel: String

    protected abstract val headerFragmentText: String
    protected open val headerSecondLine: String = ""
    protected open val headerThirdLine: String = ""

    internal var nextAction: MenuItem? = null
    lateinit var headerFragment: CreateIdentityHeaderFragment

    protected var step = 0
    var activityResult = Activity.RESULT_CANCELED

    //Indicated that the wizard is cancelable
    protected var canExit = false

    interface NextChecker {
        fun check(view: View): String?
    }

    abstract fun completeWizard()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        setContentView(R.layout.activity_create_identity)
        createFragments()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
        toolbar.setTitle(actionBarTitle)
        if (!canExit) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun createFragments() {
        headerFragment = CreateIdentityHeaderFragment()
        val ft = fragmentManager.beginTransaction()
        ft.add(R.id.fragment_container_content, fragments[0])
        ft.add(R.id.fragment_container_header, headerFragment)
        ft.commit()
    }

    protected fun checkEMailAddress(editText: String): String? {
        val error = editText.length < 1
        if (error) {
            return getString(R.string.create_identity_enter_all_data)
        }
        if (!Formatter.isEMailValid(editText)) {
            return getString(R.string.email_address_invalid)
        }
        return null
    }

    override fun onBackPressed() {

        val fragmentCount = fragmentManager.backStackEntryCount
        //check backstack if fragments exists
        if (step < fragments.size && fragmentCount > 0) {

            //check if last fragment displayed
            if (fragmentCount == fragments.size - 1) {
                //complete wizard
                activityResult = Activity.RESULT_OK
                completeWizard()

                return
            }
            //otherwise, popbackstack and update ui
            if (step > 0) {
                step--
            }
            if (step == 0 && !canExit) {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                supportActionBar?.setDisplayUseLogoEnabled(true)
            }
            fragmentManager.popBackStack()
            headerFragment.updateUI(headerFragmentText)
            updateActionBar(step)
        } else {
            //return without finish the wizard
            if (canExit) {
                finish()
            } else {
                UIHelper.showDialogMessage(this, getString(R.string.dialog_headline_warning), String.format(getString(R.string.message_step_is_needed_or_close_app), wizardEntityLabel), R.string.yes, R.string.no, { dialog, which -> finish() }) { dialog, which -> }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.ab_create_identity, menu)
        nextAction = menu.findItem(R.id.action_next) as MenuItem
        updateActionBar(step)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.action_next) {
            handleNextClick()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    open fun handleNextClick() {

        val check = (fragmentManager.findFragmentById(R.id.fragment_container_content) as BaseIdentityFragment).check()
        //check if fragment ready to go to the next step
        if (check != null) {
            //no, show error message
            UIHelper.showDialogMessage(this, R.string.dialog_headline_info, check)
        } else {

            //check if currently last step
            if (step == fragments.size - 1) {
                activityResult = Activity.RESULT_OK
                completeWizard()
                return
            }
            //no... go to next step

            if (canShowNext(step)) {
                showNextFragment()
            }
        }
    }

    /**
     * override this if you need special handling on next click

     * @param step
     * *
     * @return
     */
    protected open fun canShowNext(step: Int): Boolean {
        return true
    }

    protected fun showNextFragment() {
        step++
        if (step == fragments.size - 1) {
            canExit = true
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(false)

        headerFragment.updateUI(headerFragmentText, headerSecondLine, headerThirdLine)
        fragmentManager.beginTransaction().replace(R.id.fragment_container_content, fragments[step]).addToBackStack(null).commit()
        updateActionBar(step)
    }

    /**
     * refresh actionbar depends from current wizard state

     * @param step current step number
     */
    protected open fun updateActionBar(step: Int) {

        //update icons
        if (step == 0) {
            nextAction?.isVisible = false
        } else if (step < fragments.size - 1) {
            nextAction?.isVisible = true
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setHomeButtonEnabled(true)
            nextAction?.setTitle(R.string.next)
        } else {
            nextAction?.isVisible = true
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            nextAction?.setTitle(R.string.finish)
        }

        //update subtitle
        if (step == 0) {
            supportActionBar?.subtitle = null
        } else if (step < fragments.size - 1) {
            actionBar?.subtitle = getString(R.string.step_x_from_y).
                    replace("$1", step.toString()).
                    //First and last excluded -> -2
                    replace("$2", (fragments.size - 2).toString())
        }
    }

}
