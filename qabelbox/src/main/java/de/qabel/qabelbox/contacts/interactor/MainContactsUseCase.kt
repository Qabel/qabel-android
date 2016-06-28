package de.qabel.qabelbox.contacts.interactor

import de.qabel.core.config.Contact
import de.qabel.core.config.ContactExportImport
import de.qabel.core.config.Identity
import de.qabel.desktop.repository.ContactRepository
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.dto.ContactParseResult
import de.qabel.qabelbox.contacts.dto.ContactsParseResult
import de.qabel.qabelbox.exceptions.QblStorageEntityExistsException
import rx.Subscriber
import rx.lang.kotlin.observable
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject


class MainContactsUseCase @Inject constructor(private val activeIdentity: Identity,
                                              private val contactRepository: ContactRepository) : ContactsUseCase {

    override fun search(filter: String) = observable<ContactDto> { subscriber ->
        load(subscriber, filter)
    }

    override fun load() = observable<ContactDto> { subscriber ->
        load(subscriber, null)
    }

    private fun load(subscriber: Subscriber<in ContactDto>, filter: String?) {
        contactRepository.findWithIdentities(filter).map {
            pair ->
            subscriber.onNext(ContactDto(pair.first, pair.second,
                    !pair.second.none { identity -> identity.equals(activeIdentity) }))
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

    override fun exportContact(contactKey: String, targetFile: FileDescriptor) = observable<Int> { subscriber ->
        val contact = contactRepository.findByKeyId(activeIdentity, contactKey);
        FileOutputStream(targetFile).use({ stream ->
            stream.bufferedWriter().use { writer ->
                writer.write(ContactExportImport.exportContact(contact))
            }
            subscriber.onNext(1);
            subscriber.onCompleted();
        });
    }

    override fun exportAllContacts(targetFile: FileDescriptor) = observable<Int> { subscriber ->
        val contacts = contactRepository.findWithIdentities(null).map { pair -> pair.first };
        FileOutputStream(targetFile).use({ stream ->
            stream.bufferedWriter().use { writer ->
                writer.write(ContactExportImport.exportContactsToJSON(contacts))
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

        val contacts = ContactExportImport.importContactFromJson(inputString);
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
        val contact = ContactExportImport.importFromContactString(contactString);
        if (!contactRepository.exists(contact)) {
            contactRepository.save(contact, activeIdentity)
        }
        subscriber.onNext(ContactParseResult(contact, true));
        subscriber.onCompleted();
    }

}
