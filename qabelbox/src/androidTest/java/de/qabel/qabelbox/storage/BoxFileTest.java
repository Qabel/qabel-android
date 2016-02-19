package de.qabel.qabelbox.storage;

import android.os.Bundle;
import android.test.AndroidTestCase;

import org.junit.Test;


public class BoxFileTest extends AndroidTestCase {

	private static final String NAME = "BoxFile";
	private static final String BLOCK = "Block";
	private static final Long SIZE = 1000L;
	private static final Long MTIME = 101010101L;
	private static final byte[] KEY = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};

	@Test
	public void testBoxFileParcelable() {
		BoxFile boxFile = new BoxFile(NAME, BLOCK, SIZE, MTIME, KEY);
		Bundle bundle = new Bundle();
		bundle.putParcelable("FILE", boxFile);

		BoxFile boxFileFromBundle = bundle.getParcelable("FILE");

		assertEquals(boxFile, boxFileFromBundle);
	}
}