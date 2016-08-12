package de.qabel.qabelbox.chat.notifications

import android.content.Context
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.extensions.findById
import de.qabel.core.util.DefaultHashMap
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.notifications.presenter.ChatNotificationPresenter
import de.qabel.qabelbox.contacts.extensions.displayName
import javax.inject.Inject

class MainChatNotificationManager : ChatNotificationManager {

    internal lateinit var chatNotificationPresenter: ChatNotificationPresenter
    internal lateinit var context: Context

    @Inject
    constructor(chatNotificationPresenter: ChatNotificationPresenter,
                context: Context) : super() {
        this.chatNotificationPresenter = chatNotificationPresenter
        this.context = context
    }

    private val notifiedMap = DefaultHashMap<Identity, MutableList<ChatMessage>>({ mutableListOf() })

    override fun updateNotifications(receivedMessagesMap: Map<Identity, List<ChatMessage>>) {
        for ((identity, messages) in receivedMessagesMap) {
            val identityMessages = filterDuplicated(identity, messages)
            constructNotifications(identityMessages).forEach {
                chatNotificationPresenter.showNotification(it)
            }
            notifiedMap.getOrDefault(identity).addAll(identityMessages)
        }
    }

    override fun hideNotification(identityKey: String, contactKey: String?) {
        if (contactKey != null) {
            chatNotificationPresenter.hideNotification(identityKey, contactKey)
        } else {
            chatNotificationPresenter.hideNotification(identityKey)
        }
    }


    private fun filterDuplicated(identity: Identity, messages: List<ChatMessage>): MutableList<ChatMessage> =
            messages.filter { msg ->
                !notifiedMap.getOrDefault(identity).any {
                    it.contact == msg.contact &&
                            it.identity == msg.identity &&
                            it.time == it.time &&
                            it.direction == it.direction &&
                            it.messagePayload.toMessage() == it.messagePayload.toMessage()
                }
            }.toMutableList()

    fun constructNotifications(messages: MutableList<ChatMessage>): List<ChatNotification> {
        if (messages.size == 0) {
            return mutableListOf()
        }
        messages.sortBy { it.time }

        val notifications = mutableListOf<ChatNotification>()

        val unknownMessages = messages.filter { it.contact.status == Contact.ContactStatus.UNKNOWN }
        messages.removeAll(unknownMessages)
        if (unknownMessages.size > 0) {
            val byUnknownContact = countMessagesByContact(unknownMessages)
            for ((contact, msgCount) in byUnknownContact) {
                val contactMessage = unknownMessages.first { it.contact == contact }
                val message = if (msgCount > 1) getMultiMsgLabel(msgCount)
                else contactMessage.messagePayload.toMessage()

                notifications.add(ContactChatNotification(unknownMessages.first().identity,
                        contact, message, contactMessage.time).apply {
                    extraNotification = true
                })
            }
        }
        if (messages.size == 0) {
            return notifications
        }

        val first = messages.first()
        val byContact = countMessagesByContact(messages)
        notifications.add(
                if (byContact.size > 1) {
                    val header = createContactsHeader(byContact)
                    val body = getMultiMsgLabel(messages.size)
                    MessageChatNotification(first.identity, header, body, first.time)
                } else {
                    if (messages.size > 1) {
                        val body = context.getString(R.string.new_messages, messages.size)
                        ContactChatNotification(first.identity, first.contact,
                                body, first.time)
                    } else {
                        ContactChatNotification(first.identity, first.contact,
                                first.messagePayload.toMessage(), first.time)
                    }
                })
        return notifications
    }

    private fun getMultiMsgLabel(count: Int) = context.getString(R.string.new_messages, count)

    private fun countMessagesByContact(messages: List<ChatMessage>): DefaultHashMap<Contact, Int> =
            DefaultHashMap<Contact, Int>({ 0 }).apply {
                messages.forEach {
                    val c = keys.findById(it.contact.id) ?: it.contact
                    put(c, getOrDefault(c).plus(1))
                }
            }

    private fun createContactsHeader(byContact: DefaultHashMap<Contact, Int>): String =
            byContact.keys.map { it.displayName() }.sorted().joinToString()

}
