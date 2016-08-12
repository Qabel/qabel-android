package de.qabel.qabelbox.chat.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.support.v7.app.NotificationCompat
import dagger.internal.Factory
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.UITest
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.chat.notifications.presenter.AndroidChatNotificationPresenter
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class AndroidChatNotificationPresenterTest : UITest {

    override val context: Context
        get() = RuntimeEnvironment.application

    private lateinit var identity: Identity
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: ChatNotification
    private lateinit var presenter: AndroidChatNotificationPresenter
    private lateinit var builder: NotificationCompat.Builder

    @Before
    @Throws(Exception::class)
    fun setUp() {
        identity = IdentityHelper.createIdentity("identity", null)
        notificationManager = RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notification = MessageChatNotification(identity,
                "header, header", "message",
                Date())
        presenter = AndroidChatNotificationPresenter(RuntimeEnvironment.application,
                Factory<android.support.v7.app.NotificationCompat.Builder> {
                    builder = spy(NotificationCompat.Builder(RuntimeEnvironment.application))
                    builder
                })
    }

    @Test
    fun testShowNotification() {
        presenter.showNotification(notification)
        val note = shadowOf(notificationManager).allNotifications[0]
        assertThat(note.category, equalTo(Notification.CATEGORY_MESSAGE))
        assertThat(note.`when`, equalTo(notification.date.time))
        assertThat(note.contentIntent, notNullValue())
        assertEquals(shadowOf(note).contentText, notification.message)
        verify(builder).setDefaults(Notification.DEFAULT_ALL)
        assertThat(note.actions[0].title.toString(), equalTo(getString(R.string.action_read)))
    }

    @Test
    @Ignore("action not working in test, result notifications contains only 1 action")
    fun testShowNewContactNotification() {
        val contact = IdentityHelper.createContact("Mr. New")
        contact.status == Contact.ContactStatus.UNKNOWN
        val newNotification = ContactChatNotification(identity, contact, "Huhu", Date())
        presenter.showNotification(newNotification)
        val note = shadowOf(notificationManager).allNotifications[0]
        assertThat(note.actions.toList(), hasSize(2))
        val addAction = note.actions.find { it.title.equals(getString(R.string.add)) }
        val ignoreAction = note.actions.find { it.title.equals(getString(R.string.action_ignore)) }
        assertThat(addAction, notNullValue())
        assertThat(ignoreAction, notNullValue())
        assertThat(ignoreAction!!.icon, equalTo(R.drawable.close))
        assertThat(addAction!!.icon, equalTo(R.drawable.account_multiple_plus))
    }

    @Test
    fun testTaggedNotification() {
        presenter.showNotification(notification)
        val notification = shadowOf(notificationManager).allNotifications[0]
        val actual = shadowOf(notificationManager).getNotification("ChatNotification", 0)
        assertThat(actual, equalTo(notification))
    }

    @Test
    fun testNotificationShowsList() {
        val notification = MessageChatNotification(IdentityHelper.createIdentity("identity", null),
                "foo, bar", "message", this.notification.date)
        val intent = presenter.getChatIntent(notification)
        assertThat(intent.getStringExtra(MainActivity.ACTIVE_IDENTITY),
                equalTo(notification.identity.keyIdentifier))
        assertThat(intent.getBooleanExtra(MainActivity.START_CONTACTS_FRAGMENT, false), `is`(true))
        assertThat(intent.getStringExtra(MainActivity.ACTIVE_CONTACT), nullValue())
    }

    @Test
    fun testNotificationShowsChat() {
        val notification = ContactChatNotification(
                IdentityHelper.createIdentity("identity", null),
                IdentityHelper.createContact("contact"),
                "message", this.notification.date)
        val intent = presenter.getChatIntent(notification)
        assertThat(intent.getStringExtra(MainActivity.ACTIVE_IDENTITY),
                equalTo(notification.identity.keyIdentifier))
        assertThat(intent.getBooleanExtra(MainActivity.START_CONTACTS_FRAGMENT, false), `is`(true))
        assertThat(intent.getStringExtra(MainActivity.ACTIVE_CONTACT),
                equalTo(notification.contact.keyIdentifier))
    }

    @Test
    fun testHideMessageNotification() {
        val shadowNotificationManager = shadowOf(notificationManager)
        presenter.showNotification(notification)
        presenter.hideNotification(notification.identity.keyIdentifier)
        assertThat(shadowNotificationManager.size(), equalTo(0))
    }

    @Test
    fun testHideContactNotification() {
        val shadowNotificationManager = shadowOf(notificationManager)
        val identity = IdentityHelper.createIdentity("identity", null)
        val contact = IdentityHelper.createContact("contact")

        val contactNotification = ContactChatNotification(identity,
                contact, "message", Date())
        contactNotification.extraNotification = true

        presenter.showNotification(notification)
        presenter.showNotification(contactNotification)
        assertThat(shadowNotificationManager.size(), equalTo(2))
        presenter.hideNotification(identity.keyIdentifier, contact.keyIdentifier)
        assertThat(shadowNotificationManager.size(), equalTo(1))
        val existing = shadowNotificationManager.allNotifications[0]
        assertThat(existing.`when`, equalTo(notification.date.time))
    }


}
