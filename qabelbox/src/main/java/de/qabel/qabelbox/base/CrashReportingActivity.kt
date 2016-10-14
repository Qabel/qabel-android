package de.qabel.qabelbox.base

import android.os.Bundle
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.reporter.CrashReporter
import javax.inject.Inject

open class CrashReportingActivity : BaseActivity(), QabelLog {

    @Inject lateinit var crashReporter: CrashReporter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationComponent.inject(this)
    }

    public override fun onResume() {
        super.onResume()
        crashReporter.installCrashReporter()
    }

}
