package de.qabel.qabelbox.reporting;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.RoboApplication;
import de.qabel.qabelbox.activities.CrashReportingActivity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = RoboApplication.class, constants = BuildConfig.class)
public class CrashReportingTest {

    @Config(packageName = "de.qabel.qabel.debug")
    @Test
    public void testReportingDisabled() {
        Context context = RuntimeEnvironment.application;
        String name = context.getPackageName();
        assertThat(name + " is not a debug package",
                CrashReportingActivity.isDebugBuild(context), is(true));
    }
}
