package de.qabel.qabelbox.reporting;

import android.content.Context;
import android.test.InstrumentationTestCase;

import org.junit.Test;

import de.qabel.qabelbox.activities.CrashReportingActivity;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

public class CrashReportingTest extends InstrumentationTestCase {

    @Test
    public void testReportingDisabled() {
        Context context = getInstrumentation().getTargetContext();
        String name = context.getPackageName();
        assertThat(name + " is not a debug package",
                CrashReportingActivity.isDebugBuild(context), is(true));
    }
}
