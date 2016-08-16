package de.qabel.qabelbox.navigation

import android.app.Activity
import android.app.Fragment
import android.util.Log
import de.qabel.qabelbox.R
import org.jetbrains.anko.onUiThread

open class AbstractNavigator {

    protected fun showFragment(activity: Activity, fragment: Fragment, tag: String, addToBackStack: Boolean, waitForTransaction: Boolean) {
        val fragmentTransaction = activity.fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment, tag)
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(tag)
        }
        fragmentTransaction.commit()
        activity.onUiThread {
            if (waitForTransaction) {
                try {
                    while (activity.fragmentManager.executePendingTransactions()) {
                        Thread.sleep(50)
                    }
                } catch (e: InterruptedException) {
                    Log.e(activity.javaClass.simpleName, "Error waiting for fragment change", e)
                }

            }
        }
    }
}
