package de.qabel.qabelbox.chat.interactor

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.repository.ChatDropMessageRepository
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*


class MainChatServiceUseCase(private val chatDropMessageRepository: ChatDropMessageRepository,
                             private val contactRepository: ContactRepository,
                             private val identityRepo: IdentityRepository,
                             private val msgTransformer: ChatMessageTransformer) : ChatServiceUseCase, AnkoLogger {

    override fun markIdentityMessagesRead(identityKey: String) {
        val identity = identityRepo.find(identityKey)
        contactRepository.find(identity).entities.forEach {
            chatDropMessageRepository.markAsRead(it, identity)
        }
        info("Mark messaged read for identity " + identity.alias)
    }

    override fun markContactMessagesRead(identityKey: String, contactKey: String) {
        val identity = identityRepo.find(identityKey)
        val contact = contactRepository.findByKeyId(contactKey)
        chatDropMessageRepository.markAsRead(contact, identity)
        info("Mark messaged read for contact " + contact.alias)
    }

    override fun addContact(identityKey: String, contactKey: String) {
        val identity = identityRepo.find(identityKey)
        val contact = contactRepository.findByKeyId(identity, contactKey)
        contact.status = Contact.ContactStatus.NORMAL
        contactRepository.save(contact, identity)
        info("Contact added " + contact.alias)
    }

    override fun ignoreContact(identityKey: String, contactKey: String) {
        val identity = identityRepo.find(identityKey)
        val contact = contactRepository.findByKeyId(identity, contactKey)
        contact.isIgnored = true
        if(contact.status == Contact.ContactStatus.UNKNOWN){
            contact.status = Contact.ContactStatus.NORMAL
        }
        contactRepository.save(contact, identity)
        info("Contact ignored " + contact.alias)
    }

    override fun getNewMessageAffectedKeyIds(): Collection<String> {
        val keys = HashSet<String>()
        identityRepo.findAll().entities.map {
            keys.add(it.keyIdentifier)
            chatDropMessageRepository.findNew(it.id).forEach {
                keys.add(contactRepository.find(it.contactId).keyIdentifier)
            }
        }
        return keys
    }

    override fun getNewMessageMap(): Map<Identity, List<ChatMessage>> =
            identityRepo.findAll().entities.map {
                Pair(it, chatDropMessageRepository.findNew(it.id).map { msgTransformer.transform(it) })
            }.toMap()


}
