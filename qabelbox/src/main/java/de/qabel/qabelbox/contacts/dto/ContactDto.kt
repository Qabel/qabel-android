package de.qabel.qabelbox.contacts.dto

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity

data class ContactDto(val contact: Contact, var identities: List<Identity>, val active : Boolean = true) {

}
