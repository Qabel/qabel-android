package de.qabel.qabelbox.base

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import de.qabel.core.logging.QabelLog
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.helper.Sanity
import javax.inject.Inject

class SanityCheckStartup @Inject constructor(
        val activity: AppCompatActivity,
        val identityRepository: IdentityRepository
): ActivityStartup, QabelLog {

    override fun onCreate(): Boolean {
        if (!Sanity.isQabelReady(activity, identityRepository)) {
            debug("started wizard dialog")
            return false
        }
        return true
    }

}

