package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.external.ExternalAction
import java.io.File
import java.io.FileDescriptor

interface ContactsPresenter {

    var externalAction: ExternalAction?

    fun refresh()

    fun handleClick(contact: ContactDto)
    fun handleLongClick(contact: ContactDto) : Boolean

    fun deleteContact(contact: ContactDto)
    fun sendContact(contact: ContactDto, cacheDir: File)

    fun handleExternalFileAction(externalAction: ExternalAction, target: FileDescriptor)
    fun handleScanResult(externalAction: ExternalAction, result: String)

    fun startContactExport(contact: ContactDto)
    fun startContactsExport()

    fun startContactsImport()
    fun startContactImportScan(requestCode: Int)

}
