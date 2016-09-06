package de.qabel.qabelbox.index

import de.qabel.core.index.FieldType

//TODO coreable
data class RawContact(val displayName: String,
                      val mobilePhoneNumbers: MutableList<String>,
                      val emailAddresses: MutableList<String>,
                      val identifier: String //external identifier
)

enum class EntryStatus {
    MODIFIED,
    PENDING,
    VERIFIED
}

// Directly add verified flags to identity
//TODO OR
data class IdentityIndexStatus(val identityId: Int, val emailStatus: EntryStatus, val phoneStatus: EntryStatus)

// TODO OR
data class IndexEntry(val value: String, val type: FieldType, val status: EntryStatus)
