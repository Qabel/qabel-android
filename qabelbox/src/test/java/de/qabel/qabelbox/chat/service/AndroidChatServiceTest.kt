package de.qabel.qabelbox.chat.service

import android.content.Context
import android.content.Intent
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.core.config.Identity
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class AndroidChatServiceTest() {

    lateinit var androidService: TestChatService

    class TestChatService : AndroidChatService() {
        override fun onHandleIntent(intent: Intent) {
            super.onHandleIntent(intent)
        }

        override fun getApplicationContext(): Context {
            return RuntimeEnvironment.application
        }
    }

    val identity: Identity = IdentityHelper.createIdentity("identity", null)
    val contactKey: String = "thisIsAKey"
    val identityKey: String = "anOtherKey"

    @Before
    fun setUp() {
        RuntimeEnvironment.application.startService(Intent(RuntimeEnvironment.application, AndroidChatService::class.java))
        androidService = TestChatService()
        androidService.chatService = mock()
        androidService.chatMessageTransformer = mock()
        androidService.chatNotificationManager = mock()
    }

    @Test
    fun testStart() {
        assertThat(androidService, notNullValue())
    }

    @Test
    fun testHandleAddContact() {
        val intent = Intent(QblBroadcastConstants.Chat.Service.ADD_CONTACT).apply {
            putExtra(AndroidChatService.PARAM_CONTACT_KEY, contactKey)
            putExtra(AndroidChatService.PARAM_IDENTITY_KEY, identityKey)
        }
        androidService.onHandleIntent(intent)
        verify(androidService.chatService).addContact(identityKey, contactKey)
        verify(androidService.chatNotificationManager).hideNotification(identityKey, contactKey)
    }

    @Test
    fun testHandleIgnoreContact() {
        val intent = Intent(QblBroadcastConstants.Chat.Service.IGNORE_CONTACT).apply {
            putExtra(AndroidChatService.PARAM_CONTACT_KEY, contactKey)
            putExtra(AndroidChatService.PARAM_IDENTITY_KEY, identityKey)
        }
        androidService.onHandleIntent(intent)
        verify(androidService.chatService).ignoreContact(identityKey, contactKey)
        verify(androidService.chatNotificationManager).hideNotification(identityKey, contactKey)
    }

    @Test
    fun testHandleNotify() {
        val intent = Intent(QblBroadcastConstants.Chat.Service.NOTIFY)
        val messageMap = mapOf(Pair(identity, mock<List<ChatMessage>>()))
        whenever(androidService.chatService.getNewMessageMap()).thenReturn(messageMap)
        androidService.onHandleIntent(intent)
        verify(androidService.chatNotificationManager).updateNotifications(messageMap)
    }

    @Test
    fun testHandleShowNotification() {
        androidService.onHandleIntent(Intent(QblBroadcastConstants.Chat.Service.MESSAGES_UPDATED))
        verify(androidService.chatService).getNewMessageAffectedKeyIds()
    }

    @Test
    fun testHandleUnknownIntent() {
        androidService.onHandleIntent(Intent("Blubb Blubb"))
        assert(true)
    }

}
