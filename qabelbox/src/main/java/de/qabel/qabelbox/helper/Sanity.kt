package de.qabel.qabelbox.helper

import android.app.Activity
import android.content.Intent
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.startup.activities.CreateAccountActivity
import de.qabel.qabelbox.startup.activities.CreateIdentityActivity

object Sanity {

    /**
     * start wizard activities if app not ready to go. If other activity need to start, the current activity finished
     * @param activity   current activity
     * *
     * @return true if no wizard start needed
     */
    fun isQabelReady(activity: Activity, identityRepository: IdentityRepository): Boolean {
        if (!isAccountLoggedIn(activity)) {
            return startActivity(activity, CreateAccountActivity::class.java)
        } else {
            val identities = identityRepository.findAll().entities
            if (identities.size == 0) {
                return startActivity(activity, CreateIdentityActivity::class.java)
            }
        }
        return true
    }

    private fun isAccountLoggedIn(activity: Activity): Boolean =
            AppPreference(activity).let {
                (!it.accountName.isNullOrBlank() && !it.token.isNullOrBlank())
            }

    private fun <T : Activity> startActivity(activity: Activity, targetActivity: Class<T>): Boolean {
        val intent = Intent(activity, targetActivity)
        activity.startActivity(intent)
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        activity.finish()
        return false
    }

}
