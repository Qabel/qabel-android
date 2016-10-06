package de.qabel.qabelbox.fragments

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.view.inputmethod.InputMethodManager
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.dagger.HasComponent
import de.qabel.qabelbox.listeners.IdleCallback
import de.qabel.qabelbox.listeners.IntentListener
import de.qabel.qabelbox.listeners.toIntentFilter
import de.qabel.qabelbox.ui.QblView
import org.jetbrains.anko.ctx
import org.jetbrains.anko.longToast
import org.jetbrains.anko.onUiThread


abstract class BaseFragment(protected val mainFragment: Boolean = false,
                            protected val showOptionsMenu: Boolean = false,
                            protected val showFABButton: Boolean = false) : Fragment(), QblView {

    protected var actionBar: ActionBar? = null

    protected var mActivity: MainActivity? = null

    protected var idle: IdleCallback? = null

    protected val intentListeners = emptyList<IntentListener>()

    fun setIdleCallback(idle: IdleCallback?) {
        this.idle = idle
    }

    fun busy() = idle?.let { it.busy() }

    fun idle() = idle?.let { it.idle() }

    @Suppress("UNCHECKED_CAST")
    protected fun <C> getComponent(componentType: Class<C>): C {
        return componentType.cast((activity as HasComponent<C>).component)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActivity = activity as MainActivity
        actionBar = mActivity?.supportActionBar
    }

    /**
     * @return title for fragment
     */
    open val title: String
        get() = getString(R.string.app_name)

    open val subtitle: String? = null

    override fun onDetach() {
        super.onDetach()
        mActivity = null
        actionBar = null
    }

    override fun onResume() {
        super.onResume()
        refreshToolbarTitle()
        if (mainFragment) {
            configureAsMainFragment()
        } else {
            configureAsSubFragment()
        }

        if (showFABButton) {
            mActivity?.fab?.show()
        } else {
            mActivity?.fab?.hide()
        }
        setHasOptionsMenu(showOptionsMenu)

        intentListeners.forEach {
            ctx.registerReceiver(it.receiver, it.toIntentFilter())
        }
    }

    override fun onPause() {
        intentListeners.forEach {
            ctx.unregisterReceiver(it.receiver)
        }
        try {
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (ex: Throwable) {
            error("Error closing keyboard!", ex)
        }
        super.onPause()
    }

    protected fun refreshToolbarTitle() {
        actionBar?.apply {
            this.title = this@BaseFragment.title
            this.subtitle = this@BaseFragment.subtitle
        }
    }

    protected fun configureAsSubFragment() {
        mActivity?.toggle?.isDrawerIndicatorEnabled = false
        actionBar?.setDisplayHomeAsUpEnabled(true)
        setActionBarBackListener()
    }

    protected fun configureAsMainFragment() {
        actionBar?.setDisplayHomeAsUpEnabled(false)
        mActivity?.toggle?.isDrawerIndicatorEnabled = true
    }

    protected fun setActionBarBackListener() {
        mActivity?.toggle?.setToolbarNavigationClickListener { mActivity?.onBackPressed() }
    }

    /**
     * handle hardware back button
     *
     * @return true if the event was handled
     */
    open fun onBackPressed(): Boolean {
        return false
    }

    open fun handleFABAction(): Boolean {
        return false
    }

    override fun showDefaultError(throwable: Throwable) {
        onUiThread {
            longToast(throwable.message ?: "Error")
            error("Error", throwable)
        }
    }
}
