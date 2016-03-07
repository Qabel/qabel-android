package de.qabel.qabelbox.helper;

import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

import de.qabel.qabelbox.activities.MainActivity;

import static junit.framework.Assert.assertEquals;

public class FileHelperTest {
	@Rule
	public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, true);

	private static final String CONTENT = "This a Testfile.";

	@Test
	public void testLoadFileFromAssets(){
		String file = FileHelper.loadFileFromAssets(mActivityTestRule.getActivity(), "html/help/test.html");
		assertEquals(CONTENT, file);
	}
}
