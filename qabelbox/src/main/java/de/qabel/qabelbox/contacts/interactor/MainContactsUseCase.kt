package de.qabel.qabelbox.contacts.interactor

import de.qabel.core.config.Contact
import de.qabel.core.config.ContactExportImport
import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.contacts.ContactData
import de.qabel.core.contacts.ContactExchangeFormats
import de.qabel.core.extensions.contains
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.exception.EntityExistsException
import de.qabel.qabelbox.config.QabelSchema
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.dto.ContactParseResult
import de.qabel.qabelbox.contacts.dto.ContactsParseResult
import org.apache.commons.io.FileUtils
import rx.lang.kotlin.observable
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject


open class MainContactsUseCase @Inject constructor(private val activeIdentity: Identity,
                                                   private val contactRepository: ContactRepository,
                                                   private val identityRepository: IdentityRepository) : ContactsUseCase {

    private val contactExchangeFormats = ContactExchangeFormats()

    override fun search(filter: String, showIgnored: Boolean) = observable<ContactDto> { subscriber ->
        contactRepository.findWithIdentities(filter,
                listOf(Contact.ContactStatus.NORMAL, Contact.ContactStatus.VERIFIED),
                !showIgnored).map { pair ->
            subscriber.onNext(transformContact(pair))
        }
        subscriber.onCompleted()
    }

    private fun transformContact(data: ContactData)
            = ContactDto(data.contact, data.identities, data.identities.contains(activeIdentity.keyIdentifier))

    override fun loadContact(keyIdentifier: String) = observable<ContactDto> { subscriber ->
        val contact = contactRepository.findContactWithIdentities(keyIdentifier)
        subscriber.onNext(transformContact(contact))
        subscriber.onCompleted()
    }

    override fun loadContactAndIdentities(keyIdentifier: String) = observable<Pair<ContactDto, Identities>> {
        val identities = identityRepository.findAll()
        val contact = contactRepository.findContactWithIdentities(keyIdentifier)
        it.onNext(Pair(transformContact(contact), identities))
        it.onCompleted()
    }

    override fun deleteContact(contact: Contact) = observable<Unit> { subscriber ->
        contactRepository.delete(contact, activeIdentity)
        subscriber.onNext(Unit)
        subscriber.onCompleted()
    }

    override fun exportContact(contactKey: String, targetDirectory: File) = observable<File> { subscriber ->
        val contact = contactRepository.findByKeyId(contactKey)
        File(targetDirectory, QabelSchema.createContactFilename(contact.alias)).let {
            file ->
            FileUtils.writeStringToFile(file,
                    contactExchangeFormats.exportToContactsJSON(setOf(contact)))
            subscriber.onNext(file)
            subscriber.onCompleted()
        }
    }

    override fun exportContact(contactKey: String, targetFile: FileDescriptor) = observable<Int> { subscriber ->
        val contact = contactRepository.findByKeyId(contactKey)
        FileOutputStream(targetFile).use({ stream ->
            stream.bufferedWriter().use { writer ->
                writer.write(ContactExportImport.exportContact(contact))
            }
            subscriber.onNext(1)
            subscriber.onCompleted()
        })
    }

    override fun exportAllContacts(targetFile: FileDescriptor) = observable<Int> { subscriber ->
        val contacts = contactRepository.findWithIdentities().map { contactData -> contactData.contact }
        FileOutputStream(targetFile).use({ stream ->
            stream.bufferedWriter().use { writer ->
                writer.write(contactExchangeFormats.exportToContactsJSON(contacts.toSet()))
            }
            subscriber.onNext(contacts.size)
            subscriber.onCompleted()
        })
    }

    override fun importContacts(file: FileDescriptor) = observable<ContactsParseResult> { subscriber ->

        val inputString = FileInputStream(file).use { stream ->
            stream.bufferedReader().use { reader ->
                reader.readText()
            }
        }

        val contacts = contactExchangeFormats.importFromContactsJSON(inputString)
        var importedContacts = 0
        contacts.forEach { contact ->
            try {
                contactRepository.save(contact, activeIdentity)
                importedContacts++
            } catch(ex: EntityExistsException) {
            }
        }
        subscriber.onNext(ContactsParseResult(importedContacts, contacts.size - importedContacts))
        subscriber.onCompleted()
    }

    override fun importContactString(contactString: String) = observable<ContactParseResult> {
        subscriber ->
        val contact = contactExchangeFormats.importFromContactString(contactString)
        contactRepository.save(contact, activeIdentity)
        subscriber.onNext(ContactParseResult(contact, true))
        subscriber.onCompleted()
    }

    override fun saveContact(contact: ContactDto) = observable<Unit> {
        if (contact.contact.status == Contact.ContactStatus.UNKNOWN) {
            contact.contact.status = Contact.ContactStatus.NORMAL
        }
        contactRepository.update(contact.contact, contact.identities)
        it.onNext(Unit)
        it.onCompleted()
    }
}
