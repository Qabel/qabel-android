package de.qabel.qabelbox.ui

import android.content.Intent
import android.support.test.espresso.contrib.DrawerActions
import android.view.MenuItem
import de.qabel.core.config.Identity
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.chat.view.views.ChatFragment
import de.qabel.qabelbox.navigation.MainNavigator
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.jetbrains.anko.slidingDrawer
import org.junit.Test

class MainActivityTest : AbstractUITest() {

    @Test
    @Throws(Throwable::class)
    fun testStartWithIdentity() {
        val second = mBoxHelper.addIdentity("second")
        startWithIdentity(second)
        assertThat(mActivity.activeIdentity.keyIdentifier,
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
        intent.putExtra(MainActivity.START_CHAT_FRAGMENT, true)
        launchActivity(intent)
        val fragment = mActivity.fragmentManager.findFragmentByTag(
                MainNavigator.TAG_CONTACT_CHAT_FRAGMENT) as ChatFragment
        assertThat(fragment, notNullValue())
        assertThat(fragment.contactKeyId, equalTo(contact.keyIdentifier))
    }

    @Test
    fun testIdentityUpdated() {
        startWithIdentity(identity)
        identity.alias = "Banane"
        identity.email = "nutella@banane.de"
        mActivity.sendBroadcast(Intent(QblBroadcastConstants.Identities.IDENTITY_CHANGED).apply {
            putExtra(QblBroadcastConstants.Identities.KEY_IDENTITY, identity)
        })
        //Open drawer
        mActivity.runOnUiThread {
            mActivity.toggle.toolbarNavigationClickListener.onClick(null)
        }
        onViewVisibleText("Banane")
        onViewVisibleText("nutella@banane.de")
    }

    @Test
    fun testDeleteIdentity() {
        startWithIdentity(identity)
        identityRepository.delete(identity)
        mActivity.sendBroadcast(Intent(QblBroadcastConstants.Identities.IDENTITY_REMOVED).apply {
            putExtra(QblBroadcastConstants.Identities.KEY_IDENTITY, identity)
        })
        assert(mActivity.isFinishing)
    }

}
