package de.qabel.qabelbox.chat.notifications

import android.content.Context
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.extensions.findById
import de.qabel.core.ui.displayName
import de.qabel.core.util.DefaultHashMap
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.notifications.presenter.ChatNotificationPresenter
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
                            it.time == msg.time &&
                            it.direction == msg.direction &&
                            it.messagePayload.toMessage() == msg.messagePayload.toMessage()
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
            notifications.addAll(createNewContactNotifications(unknownMessages))
        }
        if (messages.size == 0) {
            return notifications
        }
        notifications.add(createCombinedNotification(messages))
        return notifications
    }

    private fun createCombinedNotification(messages: List<ChatMessage>): ChatNotification =
            countMessagesByContact(messages).let { byContact ->
                val firstMessage = messages.first()
                if (byContact.size > 1) {
                    val header = createContactsHeader(byContact)
                    val body = getMultiMsgLabel(messages.size)
                    MessageChatNotification(firstMessage.identity, header, body, firstMessage.time)
                } else {
                    if (messages.size > 1) {
                        val body = context.getString(R.string.new_messages, messages.size)
                        ContactChatNotification(firstMessage.identity, firstMessage.contact,
                                body, firstMessage.time)
                    } else {
                        ContactChatNotification(firstMessage.identity, firstMessage.contact,
                                firstMessage.messagePayload.toMessage(), firstMessage.time)
                    }
                }
            }

    private fun createNewContactNotifications(unknownMessages: List<ChatMessage>): List<ChatNotification> =
            countMessagesByContact(unknownMessages).map {
                val (contact, msgCount) = it
                val contactMessage = unknownMessages.first { it.contact.keyIdentifier == contact.keyIdentifier }
                val message = if (msgCount > 1) getMultiMsgLabel(msgCount)
                else contactMessage.messagePayload.toMessage()

                ContactChatNotification(unknownMessages.first().identity,
                        contact, message, contactMessage.time).apply {
                }
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
