package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Identity
import de.qabel.core.drop.DefaultDropParser
import de.qabel.core.drop.DropMessage
import de.qabel.core.drop.DropParser
import de.qabel.core.repository.ChatDropMessageRepository
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.entities.ChatDropMessage
import org.spongycastle.util.encoders.Base64
import javax.inject.Inject

class Base64DropReceiver @Inject constructor (private val parser: DropParser = DefaultDropParser(),
                                              private val repository: ChatDropMessageRepository,
                                              private val identityRepository: IdentityRepository,
                                              private val contactRepository: ContactRepository)
                         : DropReceiver {

    override fun receive(dropId: String, encodedMessage: String) {
        val decoded = Base64.decode(encodedMessage)
        val identities = identityRepository.findAll()
        val (identity, message) = parser.parse(decoded, identities)
        val chatMessage = createChatMessage(identity, message)
        repository.persist(chatMessage)
    }

    private fun createChatMessage(identity: Identity, dropMessage: DropMessage): ChatDropMessage {
        val contactId = contactRepository.findByKeyId(dropMessage.senderKeyId).id

        val type = if (dropMessage.dropPayload.equals(ChatDropMessage.MessageType.SHARE_NOTIFICATION))
            ChatDropMessage.MessageType.SHARE_NOTIFICATION else ChatDropMessage.MessageType.BOX_MESSAGE

        return ChatDropMessage(contactId, identity.id, ChatDropMessage.Direction.INCOMING,
                ChatDropMessage.Status.NEW, type, dropMessage.dropPayload, dropMessage.creationDate.time)
    }

}

