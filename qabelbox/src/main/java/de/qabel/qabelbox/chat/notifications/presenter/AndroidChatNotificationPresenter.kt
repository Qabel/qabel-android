package de.qabel.qabelbox.chat.notifications.presenter

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v7.app.NotificationCompat
import dagger.internal.Factory
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.util.DefaultHashMap
import de.qabel.qabelbox.QblBroadcastConstants.Chat.Service
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.chat.notifications.ChatNotification
import de.qabel.qabelbox.chat.notifications.ContactChatNotification
import de.qabel.qabelbox.chat.service.AndroidChatService
import javax.inject.Inject

open class AndroidChatNotificationPresenter : ChatNotificationPresenter {

    internal lateinit var context: Context
    internal lateinit var builder: Factory<NotificationCompat.Builder>

    internal var notificationManager: NotificationManager
    private var currentId = 0

    internal val identityToNotificationId = DefaultHashMap<String, Int>({ currentId++ })

    companion object {
        private val KEY = "ChatNotification"
    }

    @Inject constructor(context: Context,
                        builder: Factory<NotificationCompat.Builder>) : super() {
        this.context = context
        this.builder = builder
        this.notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun hideNotification(identityKey: String) =
        notificationManager.cancel(KEY, identityToNotificationId.getOrDefault(identityKey))


    override fun hideNotification(identityKey : String, contactKey: String) =
        notificationManager.cancel(KEY, identityToNotificationId.getOrDefault(
                identityKey + "_" + contactKey))

    override fun showNotification(notification: ChatNotification) {
        val intent = getChatIntent(notification)
        val pendingChatIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = builder.get().apply {
            setDefaults(Notification.DEFAULT_ALL)
            setWhen(notification.date.time)
            setContentIntent(pendingChatIntent)
            setContentTitle(notification.header)
            setContentText(notification.message)
            setSmallIcon(R.mipmap.ic_launcher)
            setPriority(Notification.PRIORITY_HIGH)
            setAutoCancel(true)
            setVisibility(Notification.VISIBILITY_PRIVATE)
            setCategory(Notification.CATEGORY_MESSAGE)

            when (notification) {
                is ContactChatNotification -> {
                    if (notification.contact.status == Contact.ContactStatus.UNKNOWN) {
                        addAddContactAction(notification)
                        addIgnoreContactAction(notification)
                    } else {
                        addMarkReadAction(notification)
                    }
                }
                else -> addMarkReadAction(notification)
            }
        }
        notificationManager.notify(KEY, getId(notification),
                notificationBuilder.build())
    }

    private fun getId(notification: ChatNotification): Int {
        var id = notification.identity.keyIdentifier
        if (notification is ContactChatNotification && notification.extraNotification) {
            id += "_" + notification.contact.keyIdentifier
        }
        return identityToNotificationId.getOrDefault(id)
    }

    private fun NotificationCompat.Builder.addAddContactAction(notification: ContactChatNotification) {
        addAction(R.drawable.account_multiple_plus, context.getString(R.string.add),
                PendingIntent.getService(context, 0,
                        createServiceIntent(Service.ADD_CONTACT, notification),
                        PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun NotificationCompat.Builder.addIgnoreContactAction(notification: ContactChatNotification) {
        addAction(R.drawable.close, context.getString(R.string.action_ignore),
                PendingIntent.getService(context, 0,
                        createServiceIntent(Service.IGNORE_CONTACT, notification),
                        PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun NotificationCompat.Builder.addMarkReadAction(notification: ChatNotification) {
        addAction(R.drawable.check, context.getString(R.string.action_read),
                PendingIntent.getService(context, 0,
                        createServiceIntent(Service.MARK_READ, notification),
                        PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun createServiceIntent(action: String, notification: ChatNotification) =
            Intent(context, AndroidChatService::class.java).apply {
                setAction(action)
                putExtra(AndroidChatService.PARAM_IDENTITY_KEY, notification.identity.keyIdentifier)
                if (notification is ContactChatNotification) {
                    putExtra(AndroidChatService.PARAM_CONTACT_KEY, notification.contact.keyIdentifier)
                }
            }

    fun getChatIntent(notification: ChatNotification): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.ACTIVE_IDENTITY, notification.identity.keyIdentifier)
                putExtra(MainActivity.START_CONTACTS_FRAGMENT, true)
                when (notification) {
                    is ContactChatNotification ->
                        putExtra(MainActivity.ACTIVE_CONTACT, notification.contact.keyIdentifier)
                }
            }
}
