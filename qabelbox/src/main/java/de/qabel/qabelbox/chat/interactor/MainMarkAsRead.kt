package de.qabel.qabelbox.chat.interactor

import de.qabel.chat.repository.ChatDropMessageRepository
import de.qabel.core.logging.QabelLog
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import javax.inject.Inject

class MainMarkAsRead @Inject constructor(private val chatDropMessageRepository: ChatDropMessageRepository,
                                         private val contactRepository: ContactRepository,
                                         private val identityRepo: IdentityRepository):
        MarkAsRead, QabelLog {

    override fun all(identityKey: String) {
        val identity = identityRepo.find(identityKey)
        contactRepository.find(identity).entities.forEach {
            chatDropMessageRepository.markAsRead(it, identity)
        }
        info("Mark messaged read for identity " + identity.alias)
    }

    override fun forContact(identityKey: String, contactKey: String) {
        val identity = identityRepo.find(identityKey)
        val contact = contactRepository.findByKeyId(contactKey)
        chatDropMessageRepository.markAsRead(contact, identity)
        info("Mark messaged read for contact " + contact.alias)
    }

}

