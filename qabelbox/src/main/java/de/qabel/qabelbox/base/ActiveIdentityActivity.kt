package de.qabel.qabelbox.base

interface ActiveIdentityActivity {
    val activeIdentityKey: String?
    object Constants {
        const val ACTIVE_IDENTITY = "ACTIVE_IDENTITY"
    }
}

const val ACTIVE_IDENTITY = ActiveIdentityActivity.Constants.ACTIVE_IDENTITY
