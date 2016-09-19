package de.qabel.qabelbox.util

import android.content.Context
import android.content.SharedPreferences

abstract class AndroidPreferences(context: Context) {

    abstract protected val SETTINGS_KEY: String

    protected val preferences: SharedPreferences = context.getSharedPreferences(SETTINGS_KEY,
            Context.MODE_PRIVATE)

    fun getInt(key: String, default: Int) = preferences.getInt(key, default)
    fun putInt(key: String, value: Int) = preferences.edit().putInt(key, value).apply()

    fun getLong(key: String, default: Long) = preferences.getLong(key, default)
    fun putLong(key: String, value: Long) = preferences.edit().putLong(key, value).apply()

    fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)
    fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()

    fun getString(key: String, default: String = "") = preferences.getString(key, default)
    fun putString(key: String, value: String) = preferences.edit().putString(key, value).apply()

}
