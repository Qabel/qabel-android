package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.qabelbox.contacts.dto.ContactDto
import java.io.File
import java.io.FileDescriptor

interface ContactsPresenter {

    fun refresh()

    fun deleteContact(contact : ContactDto)

    fun exportContact(contact : ContactDto)

    fun sendContact(contact: ContactDto, cacheDir: File): File?

    fun exportContacts(exportType : Int, exportKey : String, target : FileDescriptor)

    fun importContacts(target: FileDescriptor)

    fun handleScanResult(result : String)

}
