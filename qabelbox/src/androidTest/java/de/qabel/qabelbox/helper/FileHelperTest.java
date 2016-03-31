package de.qabel.qabelbox.helper;

import android.support.test.rule.ActivityTestRule;
import android.test.InstrumentationTestCase;

import org.junit.Rule;
import org.junit.Test;

import de.qabel.qabelbox.activities.MainActivity;

import static junit.framework.Assert.assertEquals;

public class FileHelperTest extends InstrumentationTestCase {

    private static final String CONTENT = "This a Testfile.";

    @Test
    public void testLoadFileFromAssets() {
        String file = FileHelper.loadFileFromAssets(getInstrumentation().getTargetContext(), "html/help/test.html");
        assertEquals(CONTENT, file);
    }
}
