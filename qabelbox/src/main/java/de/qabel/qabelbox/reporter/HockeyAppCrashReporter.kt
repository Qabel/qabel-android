package de.qabel.qabelbox.reporter

import android.content.Context
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.R
import de.qabel.qabelbox.settings.fragments.SettingsFragment
import net.hockeyapp.android.CrashManager

class HockeyAppCrashReporter (private val context: Context): CrashReporter, QabelLog {

    override fun installCrashReporter() {
        if (shouldHandleCrashes()) {
            val preferences = context.getSharedPreferences(
                    SettingsFragment.APP_PREF_NAME,
                    Context.MODE_PRIVATE)
            if (preferences.getBoolean(context.getString(R.string.settings_key_bugreporting_enabled), true)) {
                debug("install crash reporting handler")
                CrashManager.register(context, context.getString(R.string.hockeykey))
            } else {
                debug("crash reporting DISABLED")
            }
        } else {
            debug("crash reporting handler deactivated")
        }
    }

    private fun shouldHandleCrashes(): Boolean {
        return !isDebugBuild()
    }

    fun isDebugBuild(): Boolean {
        val name = context.packageName
        return name != null && name.endsWith(".debug")
    }

}
