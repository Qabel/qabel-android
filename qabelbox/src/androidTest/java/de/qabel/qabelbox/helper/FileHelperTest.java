package de.qabel.qabelbox.helper;

import android.test.InstrumentationTestCase;
import org.junit.Test;

public class FileHelperTest extends InstrumentationTestCase {

    private static final String CONTENT = "This a Testfile.";

    @Test
    public void testLoadFileFromAssets() {
        String file = FileHelper.loadFileFromAssets(getInstrumentation().getTargetContext(), "html/help/test.html");
        assertEquals(CONTENT, file);
    }
}
