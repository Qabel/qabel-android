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
import java.nio.charset.Charset
import javax.inject.Inject
import kotlin.concurrent.thread


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
        thread {
            val contacts = contactRepository.find(identityRepository.findAll(), filter).map {
                pair ->
                ContactDto(pair.first, pair.second);
            };
            contacts.map { subscriber.onNext(it) }
            subscriber.onCompleted()
        }
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

    override fun exportContact(contactKey: String, outputStream: FileOutputStream) = observable<Unit> { subscriber ->
        val contact = contactRepository.findByKeyId(identity, contactKey);
        val contactJSON = ContactExportImport.exportContact(contact);
        outputStream.bufferedWriter(Charset.defaultCharset()).write(contactJSON);
        subscriber.onNext(Unit);
        subscriber.onCompleted();
    }

    override fun exportAllContacts(identityKey: String, outputStream: FileOutputStream) = observable<Int> { subscriber ->
        val targetIdentity = identityRepository.find(identityKey);
        val contacts = contactRepository.find(targetIdentity);
        val contactsJSON = ContactExportImport.exportContacts(contacts);
        outputStream.bufferedWriter().write(contactsJSON);
        subscriber.onNext(contacts.contacts.size);
        subscriber.onCompleted();
    }

    override fun importContacts(file: FileDescriptor) = observable<ContactsParseResult> { subscriber ->
        val input = FileInputStream(file).bufferedReader().readText();
        val contacts = ContactExportImport.importContactJson(input);
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
