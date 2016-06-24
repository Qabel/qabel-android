package de.qabel.qabelbox.contacts.interactor

import de.qabel.core.config.Contact
import de.qabel.qabelbox.config.ContactExportImport
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.dto.ContactParseResult
import de.qabel.qabelbox.contacts.dto.ContactsParseResult
import rx.Observable
import java.io.FileDescriptor
import java.io.FileOutputStream


interface ContactsUseCase {

    fun load(): Observable<ContactDto>
    fun search(filter : String): Observable<ContactDto>
    fun exportContact(contactKey : String, outputStream: FileOutputStream) : Observable<Unit>
    fun exportAllContacts(identityKey : String, outputStream: FileOutputStream) : Observable<Int>
    fun importContacts(file : FileDescriptor) : Observable<ContactsParseResult>
    fun importContactString(contactString : String) : Observable<ContactParseResult>
    fun saveContact(contact : Contact): Observable<Unit>
    fun deleteContact(contact : Contact): Observable<Unit>

}
