package de.qabel.qabelbox.ui.views

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withId
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.chat.ChatMessageItem
import de.qabel.qabelbox.chat.ChatServer
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.navigation.MainNavigator
import de.qabel.qabelbox.ui.AbstractUITest
import de.qabel.qabelbox.ui.action.QabelViewAction.setText
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test


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
    }

    fun makeChatMessageItem(receiverId: String, message: String): ChatMessageItem {
        return ChatMessageItem(-1, 1, System.currentTimeMillis() + System.nanoTime() % 1000,
                identity.keyIdentifier, receiverId, message, ChatMessageItem.BOX_MESSAGE,
                ChatServer.createTextDropMessagePayload(message));
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
        val message = makeChatMessageItem(identity.keyIdentifier, "Message")
        with(ChatServer(InstrumentationRegistry.getTargetContext())) {
            storeIntoDB(identity, message)
        }
        launch()
        fragment.adapter.messages  shouldMatch hasSize(equalTo(1))
        val msg = fragment.adapter.messages[0]
        assert(msg.direction == ChatMessage.Direction.INCOMING)
    }
}
