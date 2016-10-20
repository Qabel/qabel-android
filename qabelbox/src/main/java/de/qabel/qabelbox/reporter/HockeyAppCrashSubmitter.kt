package de.qabel.qabelbox.reporter

import net.hockeyapp.android.ExceptionHandler
import java.io.IOException
import javax.inject.Inject

/**
 * Submits stracktraces to HockeyApp, but ignores IOExceptions
 */
class HockeyAppCrashSubmitter @Inject constructor(): CrashSubmitter {

    override fun submit(e: Throwable) {
        if (e !is IOException) {
            ExceptionHandler.saveException(e,
                    null, // from the current thread
                    null) // with the default crash listener from hockeyapp
        }
    }

}

