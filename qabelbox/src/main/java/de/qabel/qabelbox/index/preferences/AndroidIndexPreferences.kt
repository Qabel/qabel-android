package de.qabel.qabelbox.index.preferences

import android.content.Context
import android.content.SharedPreferences
import de.qabel.qabelbox.util.AndroidPreferences

/**
 * Uses [Int] to save state. (-1 : uninitialied, 0 : disabled, 1 enabled)
 */
class AndroidIndexPreferences(context: Context) : IndexPreferences, AndroidPreferences(context) {

    override val SETTINGS_KEY: String = KEY

    companion object {
        val KEY = "index_preferences"
        val CONTACT_SYNC = "contact_sync_enabled"
        private val CONTACT_SYNC_TIME = "contact_sync_time"

        private val PHONE_STATE_PERMISSION = "phone_state_permission"
    }

    override val contactSyncAsked: Boolean
        get() = getInt(CONTACT_SYNC, -1) >= 0

    override var contactSyncEnabled: Boolean
        get() = getInt(CONTACT_SYNC, 1) > 0
        set(value) = putInt(CONTACT_SYNC, if (value) 1 else 0)

    override var contactSyncTime: Long
        get() = getLong(CONTACT_SYNC_TIME, 0)
        set(value) = putLong(CONTACT_SYNC_TIME, value)

    override var phoneStatePermission: Boolean
        get() = getBoolean(PHONE_STATE_PERMISSION, true)
        set(value) = putBoolean(PHONE_STATE_PERMISSION, value)


}
