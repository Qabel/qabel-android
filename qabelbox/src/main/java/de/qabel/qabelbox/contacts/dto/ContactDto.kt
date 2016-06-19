package de.qabel.qabelbox.contacts.dto

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity

data class ContactDto(val contact : Contact, val identities : List<Identity>)
