package de.qabel.qabelbox.ui.views

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.ViewMatchers.*
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.dto.MessagePayload
import de.qabel.qabelbox.navigation.MainNavigator
import de.qabel.qabelbox.ui.AbstractUITest
import de.qabel.qabelbox.ui.action.QabelViewAction.setText
import de.qabel.qabelbox.ui.idling.InjectedIdlingResource
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import java.util.*


class ChatFragmentTest: AbstractUITest() {


    val contact = IdentityHelper.createContact("contact")
    lateinit var fragment: ChatFragment

    override fun setUp() {
        super.setUp()
        contactRepository.save(contact, identity)
    }

    fun launch() {
        with(defaultIntent) {
            putExtra(MainActivity.ACTIVE_CONTACT, contact.keyIdentifier)
            putExtra(MainActivity.START_CONTACTS_FRAGMENT, true)
            launchActivity(this)
        }
        fragment = mActivity.fragmentManager.findFragmentByTag(
                MainNavigator.TAG_CONTACT_CHAT_FRAGMENT) as ChatFragment

        val resource = InjectedIdlingResource()
        Espresso.registerIdlingResources(resource);
        fragment.setIdleCallback(resource)
    }

    @Test
    fun testShowEmpty() {
        launch()
        fragment.adapter.messages shouldMatch hasSize(equalTo(0))
    }

    @Test
    fun sendMessage() {
        launch()
        onView(withId(R.id.etText)).perform(setText("My Message"))
        onView(withId(R.id.bt_send)).perform(click())
        fragment.adapter.messages  shouldMatch hasSize(equalTo(1))
    }

    @Test
    fun testShowMessages() {
        val message = ChatMessage(identity, contact, ChatMessage.Direction.INCOMING, Date(),
                MessagePayload.TextMessage("MESSAGE"))
        launch()
        fragment.showMessages(listOf(message))
        onView(withText("MESSAGE")).check(ViewAssertions.matches(isDisplayed()))
    }
}
