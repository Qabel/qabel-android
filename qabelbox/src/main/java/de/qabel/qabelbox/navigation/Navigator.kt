package de.qabel.qabelbox.navigation

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.contacts.dto.ContactDto

interface Navigator {

    fun selectCreateAccountActivity()
    fun selectManageIdentitiesFragment()

    fun selectFilesFragment()

    fun selectHelpFragment()
    fun selectAboutFragment()

    fun selectContactsFragment()
    fun selectContactDetailsFragment(contactDto: ContactDto)

    fun selectChatFragment(activeContact: String?)
    fun selectContactChat(contactKey: String, withIdentity: Identity)

    fun selectQrCodeFragment(contact: Contact)

    fun selectContactEdit(contactDto: ContactDto)
    open fun popBackStack()
}
