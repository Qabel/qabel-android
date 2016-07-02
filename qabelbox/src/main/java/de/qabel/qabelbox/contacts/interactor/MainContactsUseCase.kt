package de.qabel.qabelbox.contacts.interactor

import de.qabel.core.config.Contact
import de.qabel.core.config.ContactExportImport
import de.qabel.core.config.Identity
import de.qabel.core.contacts.ContactExchangeFormats
import de.qabel.desktop.repository.ContactRepository
import de.qabel.qabelbox.config.QabelSchema
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.dto.ContactParseResult
import de.qabel.qabelbox.contacts.dto.ContactsParseResult
import org.apache.commons.io.FileUtils
import rx.Subscriber
import rx.lang.kotlin.observable
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject


class MainContactsUseCase @Inject constructor(private val activeIdentity: Identity,
                                              private val contactRepository: ContactRepository) : ContactsUseCase {

    private val contactExchangeFormats = ContactExchangeFormats();

    override fun search(filter: String) = observable<ContactDto> { subscriber ->
        load(subscriber, filter)
    }

    override fun load() = observable<ContactDto> { subscriber ->
        load(subscriber, "")
    }

    private fun load(subscriber: Subscriber<in ContactDto>, filter: String) {
        contactRepository.findWithIdentities(filter).map {
            pair ->
            subscriber.onNext(ContactDto(pair.first, pair.second,
                    !pair.second.none { identity -> identity.keyIdentifier.equals(activeIdentity.keyIdentifier) }))
        };
        subscriber.onCompleted()
    }

    override fun loadContact(keyIdentifier: String) = observable<ContactDto> { subscriber ->
        val contact = contactRepository.findContactWithIdentities(keyIdentifier);
        subscriber.onNext(ContactDto(contact.first, contact.second))
        subscriber.onCompleted();
    }

    override fun saveContact(contact: Contact) = observable<Unit> { subscriber ->
        contactRepository.save(contact, activeIdentity)
        subscriber.onNext(Unit);
        subscriber.onCompleted();
    }

    override fun deleteContact(contact: Contact) = observable<Unit> { subscriber ->
        contactRepository.delete(contact, activeIdentity)
        subscriber.onNext(Unit);
        subscriber.onCompleted();
    }

    override fun exportContact(contactKey: String, targetDirectory: File) = observable<File> { subscriber ->
        val contact = contactRepository.findByKeyId(contactKey);
        File(targetDirectory, QabelSchema.createContactFilename(contact.alias)).let {
            file ->
            FileUtils.writeStringToFile(file,
                    contactExchangeFormats.exportToContactsJSON(setOf(contact)))
            subscriber.onNext(file);
            subscriber.onCompleted();
        }
    }

    override fun exportContact(contactKey: String, targetFile: FileDescriptor) = observable<Int> { subscriber ->
        val contact = contactRepository.findByKeyId(contactKey);
        FileOutputStream(targetFile).use({ stream ->
            stream.bufferedWriter().use { writer ->
                writer.write(ContactExportImport.exportContact(contact))
            }
            subscriber.onNext(1);
            subscriber.onCompleted();
        });
    }

    override fun exportAllContacts(targetFile: FileDescriptor) = observable<Int> { subscriber ->
        val contacts = contactRepository.findWithIdentities().map { pair -> pair.first };
        FileOutputStream(targetFile).use({ stream ->
            stream.bufferedWriter().use { writer ->
                writer.write(contactExchangeFormats.exportToContactsJSON(contacts.toSet()))
            }
            subscriber.onNext(contacts.size);
            subscriber.onCompleted();
        });
    }

    override fun importContacts(file: FileDescriptor) = observable<ContactsParseResult> { subscriber ->

        val inputString = FileInputStream(file).use { stream ->
            stream.bufferedReader().use { reader ->
                reader.readText()
            }
        }

        val contacts = contactExchangeFormats.importFromContactsJSON(inputString);
        var importedContacts = 0;
        contacts.forEach { contact ->
            if (!contactRepository.exists(contact)) {
                contactRepository.save(contact, activeIdentity);
                importedContacts++;
            }
        }
        subscriber.onNext(ContactsParseResult(importedContacts, contacts.size - importedContacts));
        subscriber.onCompleted();
    }

    override fun importContactString(contactString: String) = observable<ContactParseResult> {
        subscriber ->
        val contact = contactExchangeFormats.importFromContactString(contactString);
        if (!contactRepository.exists(contact)) {
            contactRepository.save(contact, activeIdentity)
        }
        subscriber.onNext(ContactParseResult(contact, true));
        subscriber.onCompleted();
    }

}
