package de.qabel.qabelbox.index


//TODO coreable
data class RawContact(val displayName: String,
                      val mobilePhoneNumbers: List<String>,
                      val emailAddresses: List<String>,
                      val identifier: String? = null //external identifier
)
