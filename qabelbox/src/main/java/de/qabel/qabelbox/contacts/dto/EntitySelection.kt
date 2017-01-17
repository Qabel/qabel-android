package de.qabel.qabelbox.contacts.dto

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity

data class EntitySelection(val alias: String, val keyId: String) {
    constructor(identity: Identity) : this(identity.alias, identity.keyIdentifier)
    constructor(contact: Contact) : this(contact.alias, contact.keyIdentifier)

    override fun toString(): String = alias
}
