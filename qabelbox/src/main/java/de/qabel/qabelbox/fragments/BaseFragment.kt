package de.qabel.qabelbox.fragments

import android.app.Fragment
import android.os.Bundle
import android.support.v7.app.ActionBar
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.dagger.HasComponent
import de.qabel.qabelbox.listeners.IdleCallback
import java.util.concurrent.Executor
import java.util.concurrent.Executors

abstract class BaseFragment : Fragment() {
    protected var actionBar: ActionBar? = null

    protected var mActivity: MainActivity? = null

    protected var idle: IdleCallback? = null

    fun setIdleCallback(idle: IdleCallback?) {
        this.idle = idle
    }

    fun busy() = idle?.let { it.busy() }

    fun idle() = idle?.let { it.idle() }

    @SuppressWarnings("unchecked")
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

    /**
     * true if floating action button used
     */
    open val isFabNeeded = false

    override fun onDetach() {
        super.onDetach()
        mActivity = null
        actionBar = null
    }

    override fun onResume() {
        super.onResume()
        actionBar?.apply {
            this.title = this@BaseFragment.title
            this.subtitle = this@BaseFragment.subtitle
        }
        if (isFabNeeded) {
            mActivity?.fab?.show()
        } else {
            mActivity?.fab?.hide()
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
     * @return true if fragment handle back button. otherwise return false to display sideMenu icon
     */
    open fun supportBackButton(): Boolean {
        return false
    }

    /**
     * handle hardware back button
     */
    open fun onBackPressed() {

    }

    open fun handleFABAction(): Boolean {
        return false
    }
}
