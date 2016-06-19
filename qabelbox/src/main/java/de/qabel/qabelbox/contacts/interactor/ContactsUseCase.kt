package de.qabel.qabelbox.contacts.interactor

import de.qabel.core.config.Contact
import de.qabel.qabelbox.config.ContactExportImport
import de.qabel.qabelbox.contacts.dto.ContactDto
import rx.Observable
import java.io.FileDescriptor
import java.io.FileOutputStream


interface ContactsUseCase {

    fun load(): Observable<ContactDto>
    fun search(filter : String): Observable<ContactDto>
    fun exportContact(contactKey : String, outputStream: FileOutputStream) : Observable<Void>
    fun exportAllContacts(identityKey : String, outputStream: FileOutputStream) : Observable<Int>
    fun importContacts(file : FileDescriptor) : Observable<Pair<Int, Int>>
    fun saveContact(contact : Contact): Observable<Void>
    fun deleteContact(contact : Contact): Observable<Void>

}
