package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.drop.DefaultDropParser
import de.qabel.core.drop.DropMessage
import de.qabel.core.drop.DropParser
import de.qabel.core.exceptions.QblException
import de.qabel.core.repository.ChatDropMessageRepository
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.core.repository.exception.EntityNotFoundException
import de.qabel.qabelbox.chat.dto.ChatMessageInfo
import de.qabel.qabelbox.chat.notifications.ChatNotificationManager
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import org.spongycastle.util.encoders.Base64
import javax.inject.Inject

class Base64DropReceiver @Inject constructor (private val repository: ChatDropMessageRepository,
                                              private val identityRepository: IdentityRepository,
                                              private val contactRepository: ContactRepository,
                                              internal var notificationManager: ChatNotificationManager
                                              )
                         : DropReceiver, AnkoLogger {
    private val parser: DropParser = DefaultDropParser()
    override fun receive(dropId: String, encodedMessage: String) {
        val decoded = Base64.decode(encodedMessage)
        val identities = identityRepository.findAll()
        val (identity, message) = try {
            parser.parse(decoded, identities)
        } catch (e: QblException) {
            warn("Could not decrypt message", e)
            return
        }
        val contact = try {
            contactRepository.findByKeyId(message.senderKeyId)
        } catch (e: EntityNotFoundException) {
            warn("Unknown contact", e)
            return
        }
        val chatMessage = createChatMessage(identity, contact, message)
        repository.persist(chatMessage)
        info("persisting message $chatMessage")
        notificationManager.updateNotifications(
                listOf(ChatMessageInfo.fromChatDropMessage(identity, contact, chatMessage)))
    }

    private fun createChatMessage(identity: Identity, contact: Contact, dropMessage: DropMessage): ChatDropMessage {

        val type = if (dropMessage.dropPayload.equals(ChatDropMessage.MessageType.SHARE_NOTIFICATION))
            ChatDropMessage.MessageType.SHARE_NOTIFICATION else ChatDropMessage.MessageType.BOX_MESSAGE

        return ChatDropMessage(contact.id, identity.id, ChatDropMessage.Direction.INCOMING,
                ChatDropMessage.Status.NEW, type, dropMessage.dropPayload, dropMessage.creationDate.time)
    }

}

