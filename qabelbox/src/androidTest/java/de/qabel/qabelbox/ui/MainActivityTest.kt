package de.qabel.qabelbox.ui

import android.content.Intent

import org.junit.Test

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.navigation.MainNavigator
import de.qabel.qabelbox.chat.view.views.ChatFragment
import de.qabel.qabelbox.util.IdentityHelper

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions.openDrawer
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue

class MainActivityTest : AbstractUITest() {

    @Test
    @Throws(Throwable::class)
    fun testChangeActiveIdentity() {
        val second = mBoxHelper.addIdentity("second")
        startWithIdentity(identity)
        openDrawer(R.id.drawer_layout)
        onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click())
        onView(withText("second")).perform(click())
        intended(hasExtra(MainActivity.ACTIVE_IDENTITY, second.keyIdentifier))
    }

    @Test
    @Throws(Throwable::class)
    fun testStartWithIdentity() {
        val second = mBoxHelper.addIdentity("second")
        startWithIdentity(second)
        assertThat(mActivity.activeIdentity!!.keyIdentifier,
                equalTo(second.keyIdentifier))
    }

    fun startWithIdentity(identity: Identity) {
        val intent = defaultIntent
        intent.putExtra(MainActivity.ACTIVE_IDENTITY, identity.keyIdentifier)
        launchActivity(intent)
    }

    @Test
    @Throws(Throwable::class)
    fun testStartWithChat() {
        val contact = IdentityHelper.createContact("chat contact")
        contactRepository.save(contact, identity)
        val intent = defaultIntent
        intent.putExtra(MainActivity.ACTIVE_IDENTITY, identity.keyIdentifier)
        intent.putExtra(MainActivity.ACTIVE_CONTACT, contact.keyIdentifier)
        intent.putExtra(MainActivity.START_CONTACTS_FRAGMENT, true)
        launchActivity(intent)
        val fragment = mActivity.fragmentManager.findFragmentByTag(
                MainNavigator.TAG_CONTACT_CHAT_FRAGMENT) as ChatFragment
        assertThat(fragment, notNullValue())
        assertThat(fragment.contactKeyId, equalTo(contact.keyIdentifier))
    }
}
