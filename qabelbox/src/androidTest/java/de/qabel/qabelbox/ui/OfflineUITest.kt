package de.qabel.qabelbox.ui

import android.app.Activity
import android.support.test.espresso.Espresso

import org.junit.Test

import de.qabel.qabelbox.R
import de.qabel.qabelbox.ui.helper.UITestHelper

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withText
import de.qabel.qabelbox.communication.connection.ConnectivityManager
import de.qabel.qabelbox.ui.idling.InjectedIdlingResource

class OfflineUITest : AbstractUITest() {

    lateinit private var connectivityManager: MockConnectivityManager
    val idlingResource = InjectedIdlingResource()

    internal inner class MockConnectivityManager(private val context: Activity):
            ConnectivityManager(context) {

        override var isConnected = true
            set(connected) {
                field = connected
                context.runOnUiThread {
                    if (connected) {
                        listener?.handleConnectionEstablished()
                    } else {
                        listener?.handleConnectionLost()
                    }
                }
            }
    }


    @Throws(Throwable::class)
    override fun setUp() {
        super.setUp()
        launchActivity(null)
        mActivity.idleCallback = idlingResource
        Espresso.registerIdlingResources(idlingResource)

        connectivityManager = MockConnectivityManager(mActivity)
        mActivity.installConnectivityManager(connectivityManager)
        connectivityManager.isConnected = true
    }


    @Test
    @Throws(Throwable::class)
    fun testOfflineIndicator() {
        onView(withText(R.string.no_connection)).check(doesNotExist())
        connectivityManager.isConnected = false
        onView(withText(R.string.no_connection)).check(matches(isDisplayed()))
        UITestHelper.screenShot(mActivity, "offlineIndicator")
    }

    @Test
    fun testOnline() {
        onView(withText(R.string.no_connection)).check(doesNotExist())
    }

}
