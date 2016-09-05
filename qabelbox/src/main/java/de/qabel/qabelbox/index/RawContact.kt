package de.qabel.qabelbox.index

data class RawContact(val displayName: String,
                      val mobilePhoneNumbers: List<String>,
                      val emailAddresses: List<String>)
