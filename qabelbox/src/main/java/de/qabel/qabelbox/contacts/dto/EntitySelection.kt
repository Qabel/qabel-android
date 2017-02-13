package de.qabel.qabelbox.contacts.dto

import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.ui.AliasedEntity
import de.qabel.core.ui.displayName

data class EntitySelection(override val alias: String, val keyId: String) : AliasedEntity {
    constructor(identity: Identity) : this(identity.alias, identity.keyIdentifier)
    constructor(contact: Contact) : this(contact.displayName(), contact.keyIdentifier)

    override fun toString(): String = alias
}
