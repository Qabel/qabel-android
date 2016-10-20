package de.qabel.qabelbox.reporting

import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.base.CrashReportingActivity
import de.qabel.qabelbox.reporter.HockeyAppCrashReporter
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class CrashReportingTest {

    @Config(packageName = "de.qabel.qabel.debug")
    @Test
    fun testReportingDisabled() {
        val context = RuntimeEnvironment.application
        val name = context.packageName
        val reporter = HockeyAppCrashReporter(context)
        assertThat(name + " is not a debug package",
                reporter.isDebugBuild(), `is`(true))
    }
}
