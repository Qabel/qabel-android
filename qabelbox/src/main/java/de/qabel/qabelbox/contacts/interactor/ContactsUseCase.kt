package de.qabel.qabelbox.contacts.interactor

import de.qabel.core.config.Contact
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.dto.ContactParseResult
import de.qabel.qabelbox.contacts.dto.ContactsParseResult
import rx.Observable
import java.io.FileDescriptor


interface ContactsUseCase {

    fun load(): Observable<ContactDto>
    fun search(filter : String): Observable<ContactDto>
    fun loadContact(keyIdentifier : String): Observable<ContactDto>

    fun exportContact(contactKey : String, targetFile: FileDescriptor) : Observable<Int>
    fun exportAllContacts(targetFile: FileDescriptor) : Observable<Int>
    fun importContacts(file : FileDescriptor) : Observable<ContactsParseResult>
    fun importContactString(contactString : String) : Observable<ContactParseResult>
    fun saveContact(contact : Contact): Observable<Unit>
    fun deleteContact(contact : Contact): Observable<Unit>

}
