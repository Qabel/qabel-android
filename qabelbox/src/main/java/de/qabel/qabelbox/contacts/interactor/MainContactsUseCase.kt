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


class MainContactsUseCase @Inject constructor(val identity: Identity,
                                              private val identityRepository: IdentityRepository,
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
            val contactDto = ContactDto(pair.first, pair.second);
            contactDto.active = pair.second.map { ident -> ident.keyIdentifier }.contains(identity.keyIdentifier);
            subscriber.onNext(contactDto)
        };
        subscriber.onCompleted()
    }

    override fun loadContact(keyIdentifier: String) = observable<ContactDto> { subscriber ->
        val contact = contactRepository.findByKeyId(keyIdentifier);
        val contactIdentities = contactRepository.findContactIdentities(contact.keyIdentifier);
        subscriber.onNext(ContactDto(contact, contactIdentities))
        subscriber.onCompleted();
    }

    override fun saveContact(contact: Contact) = observable<Unit> { subscriber ->
        contactRepository.save(contact, identity)
        subscriber.onNext(Unit);
        subscriber.onCompleted();
    }

    override fun deleteContact(contact: Contact) = observable<Unit> { subscriber ->
        contactRepository.delete(contact, identity)
        subscriber.onNext(Unit);
        subscriber.onCompleted();
    }

    override fun exportContact(contactKey: String, targetFile: FileDescriptor) = observable<Int> { subscriber ->
        FileOutputStream(targetFile).use({ fileOutputStream ->
            val contact = contactRepository.findByKeyId(identity, contactKey);
            val contactJSON = ContactExportImport.exportContact(contact);
            val writer = fileOutputStream.bufferedWriter();
            writer.write(contactJSON);
            writer.close();
            subscriber.onNext(1);
            subscriber.onCompleted();
        });
    }

    override fun exportAllContacts(targetFile: FileDescriptor) = observable<Int> { subscriber ->
        FileOutputStream(targetFile).use({ fileOutputStream ->
            val contacts = contactRepository.findWithIdentities(null).map { pair -> pair.first };
            val contactsJSON = ContactExportImport.exportContactsToJSON(contacts);
            val writer = fileOutputStream.bufferedWriter();
            writer.write(contactsJSON);
            writer.close();
            subscriber.onNext(contacts.size);
            subscriber.onCompleted();
        });
    }

    override fun importContacts(file: FileDescriptor) = observable<ContactsParseResult> { subscriber ->
        val reader = FileInputStream(file).bufferedReader();
        val input = reader.readText();
        reader.close();
        val contacts = ContactExportImport.importContactFromJson(input);
        var importedContacts = 0;
        contacts.forEach { contact ->
            try {
                contactRepository.save(contact, identity);
                importedContacts++;
            } catch(exception: QblStorageEntityExistsException) {
                //Ignore
            }
        }
        subscriber.onNext(ContactsParseResult(importedContacts, contacts.size - importedContacts));
        subscriber.onCompleted();
    }

    override fun importContactString(contactString: String) = observable<ContactParseResult> {
        subscriber ->
        val contact = ContactExportImport.importFromContactString(contactString);
        var success = true;
        try {
            contactRepository.save(contact, identity)
        } catch(ex: QblStorageEntityExistsException) {
            success = false;
        }
        subscriber.onNext(ContactParseResult(contact, success));
        subscriber.onCompleted();
    }

}
