package de.qabel.qabelbox.index.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Uses [Int] to save state. (-1 : uninitialied, 0 : disabled, 1 enabled)
 */
class AndroidIndexPreferences(context: Context) : IndexPreferences {

    companion object {
        private val PREF_KEY = "index_settings"

        private val KEY_CONTACT_SYNC = "contact_sync_enabled"
        private val KEY_CONTACT_SYNC_TIME = "contact_sync_time"

        private val KEY_INDEX_UPLOAD_ENABLED = "index_upload_enabled"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_KEY,
            Context.MODE_PRIVATE)

    override val contactSyncAsked: Boolean
        get() = preferences.getInt(KEY_CONTACT_SYNC, -1) >= 0

    override var contactSyncEnabled: Boolean
        get() = preferences.getInt(KEY_CONTACT_SYNC, -1) > 0
        set(value) = preferences.edit().putInt(KEY_CONTACT_SYNC, if (value) 1 else 0).apply()

    override var contactSyncTime: Long
        get() = preferences.getLong(KEY_CONTACT_SYNC_TIME, 0)
        set(value) = preferences.edit().putLong(KEY_CONTACT_SYNC_TIME, value).apply()


    override val indexUploadAsked: Boolean
        get() = preferences.getInt(KEY_INDEX_UPLOAD_ENABLED, -1) >= 0

    override var indexUploadEnabled: Boolean
        get() = preferences.getInt(KEY_INDEX_UPLOAD_ENABLED, -1) > 0
        set(value) = preferences.edit().putInt(KEY_INDEX_UPLOAD_ENABLED, if (value) 1 else 0).apply()

}
