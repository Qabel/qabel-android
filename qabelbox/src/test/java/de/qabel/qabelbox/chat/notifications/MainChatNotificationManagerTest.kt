package de.qabel.qabelbox.chat.notifications

import android.content.Context
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.chat.notifications.presenter.ChatNotificationPresenter
import de.qabel.qabelbox.util.IdentityHelper
import org.apache.commons.lang3.time.DateUtils
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class MainChatNotificationManagerTest {

    private lateinit var manager: MainChatNotificationManager
    private lateinit var now: Date
    private lateinit var presenter: ChatNotificationPresenter
    private lateinit var context: Context
    private lateinit var contact: Contact
    private lateinit var identity: Identity

    @Before
    fun setUp() {
        presenter = mock(ChatNotificationPresenter::class.java)
        manager = MainChatNotificationManager(presenter, RuntimeEnvironment.application)
        now = Date()
        context = RuntimeEnvironment.application
        contact = IdentityHelper.createContact("contact")
        contact.id = 1
        identity = IdentityHelper.createIdentity("identity", null)
        identity.id = 1
    }

    @Test
    fun testUpdateNotifications() {
        val notification = ContactChatNotification(identity, contact, "message", now)
        doUpdateNotifications()
        verify<ChatNotificationPresenter>(presenter).showNotification(notification)
    }

    @Test
    fun testDuplicateNotificationsPrevented() {
        val msg = createExampleMessage(now)
        val notification = ContactChatNotification(identity, contact, "message", now)
        doUpdateNotifications(listOf(msg))
        doUpdateNotifications(listOf(msg))
        verify<ChatNotificationPresenter>(presenter).showNotification(notification)
        doUpdateNotifications(listOf(msg,
                msg.copy(messagePayload = MessagePayloadDto.TextMessage("noch eine nachricht"))))
        verify(presenter).showNotification(notification.copy(message = "noch eine nachricht"))
    }


    fun doUpdateNotifications(messages : List<ChatMessage> = listOf(createExampleMessage(now))): Map<Identity, List<ChatMessage>> =
            mapOf(Pair(identity, messages)).apply {
                manager.updateNotifications(this)
            }

    fun createExampleMessage(sent: Date, msgContact: Contact = contact, msgIdentity: Identity = identity): ChatMessage {
        return ChatMessage(
                msgIdentity,
                msgContact,
                ChatDropMessage.Direction.INCOMING,
                sent,
                MessagePayloadDto.TextMessage("message"))
    }

    @Test
    fun testMultipleMessagesFromSingleSender() {
        val messages = ArrayList<ChatMessage>()
        val message = createExampleMessage(now)
        messages.add(message)
        messages.add(createExampleMessage(DateUtils.addMinutes(now, 1)))

        val notifications = manager.constructNotifications(messages)
        val expected = ContactChatNotification(message.identity,
                message.contact, context.getString(R.string.new_messages, 2), now)
        assertThat(notifications[0], equalTo<ChatNotification>(expected))
    }

    @Test
    fun testOneMessageToDifferentIdentities() {
        val secondIdentity = IdentityHelper.createIdentity("second", null)
        secondIdentity.id = 2
        val msg = createExampleMessage(now)
        val secondMsg = createExampleMessage(now, contact, secondIdentity)
        val expected = ContactChatNotification(msg.identity,
                msg.contact, msg.messagePayload.toMessage(), now)
        val secondExpected = ContactChatNotification(secondMsg.identity,
                secondMsg.contact, secondMsg.messagePayload.toMessage(), now)

        val notification = mapOf(Pair(identity, listOf(msg)),
                Pair(secondIdentity, listOf(secondMsg)))
        manager.updateNotifications(notification)
        verify<ChatNotificationPresenter>(presenter).showNotification(expected)
        verify<ChatNotificationPresenter>(presenter).showNotification(secondExpected)
    }

    @Test
    fun testMultipleSendersToSingleIdentity() {
        val secondContact = IdentityHelper.createContact("second").apply { id == 2 }
        val msg = createExampleMessage(now)
        val secondMsg = createExampleMessage(DateUtils.addMinutes(now, 1), secondContact)
        val messages = mutableListOf(msg, secondMsg)

        val notifications = manager.constructNotifications(messages)
        assertThat(notifications, hasSize(1))
        val notification = notifications.first()
        assertThat(notification, notNullValue())
        assertThat(notification, instanceOf(MessageChatNotification::class.java))
        assertThat(notification.message, equalTo(context.getString(R.string.new_messages, 2)))
        assertThat(notification.header, equalTo<String>(contact.alias + ", " + secondContact.alias))
    }

    @Test
    fun testNoNotification() {
        assertThat(manager.constructNotifications(mutableListOf()), hasSize(0))
    }
}
