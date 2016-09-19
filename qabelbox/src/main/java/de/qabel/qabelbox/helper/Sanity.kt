package de.qabel.qabelbox.helper

import android.app.Activity
import android.content.Context
import android.content.Intent

import de.qabel.core.config.Identities
import de.qabel.core.config.Persistable
import de.qabel.core.config.VerificationStatus
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.exception.PersistenceException
import de.qabel.qabelbox.activities.BaseWizardActivity
import de.qabel.qabelbox.activities.CreateAccountActivity
import de.qabel.qabelbox.activities.CreateIdentityActivity
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.index.preferences.AndroidIndexPreferences
import de.qabel.qabelbox.permissions.DataPermissionsAdapter
import de.qabel.qabelbox.permissions.requestContactsReadPermission

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
            } else if (identities.any {
                it.phoneStatus == VerificationStatus.NOT_VERIFIED
            }) {
                //TODO Replace with verify phone actitivity
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
