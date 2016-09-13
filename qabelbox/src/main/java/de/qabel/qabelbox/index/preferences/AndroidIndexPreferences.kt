package de.qabel.qabelbox.index.preferences

import android.content.Context
import android.content.SharedPreferences
import de.qabel.qabelbox.util.AndroidPreferences

/**
 * Uses [Int] to save state. (-1 : uninitialied, 0 : disabled, 1 enabled)
 */
class AndroidIndexPreferences(context: Context) : IndexPreferences, AndroidPreferences(context) {

    override val SETTINGS_KEY: String = "index_preferences"

    companion object {

        private val CONTACT_SYNC = "contact_sync_enabled"
        private val CONTACT_SYNC_TIME = "contact_sync_time"

        private val INDEX_UPLOAD_ENABLED = "index_upload_enabled"
        private val PHONE_STATE_PERMISSION = "phone_state_permission"
        private val CONTACTS_READ_PERMISSION = "contacts_read_permission"

    }

    override val contactSyncAsked: Boolean
        get() = getInt(CONTACT_SYNC, -1) >= 0

    override var contactSyncEnabled: Boolean
        get() = getInt(CONTACT_SYNC, -1) > 0
        set(value) = putInt(CONTACT_SYNC, if (value) 1 else 0)

    override var contactSyncTime: Long
        get() = getLong(CONTACT_SYNC_TIME, 0)
        set(value) = putLong(CONTACT_SYNC_TIME, value)

    override val indexUploadAsked: Boolean
        get() = getInt(INDEX_UPLOAD_ENABLED, -1) >= 0

    override var indexUploadEnabled: Boolean
        get() = getInt(INDEX_UPLOAD_ENABLED, -1) > 0
        set(value) = putInt(INDEX_UPLOAD_ENABLED, if (value) 1 else 0)

    override var phoneStatePermission: Boolean
        get() = getBoolean(PHONE_STATE_PERMISSION, true)
        set(value) = putBoolean(PHONE_STATE_PERMISSION, value)

    override var contactsReadPermission: Boolean
        get() = getBoolean(CONTACTS_READ_PERMISSION, true)
        set(value) = putBoolean(CONTACTS_READ_PERMISSION, value)

}
