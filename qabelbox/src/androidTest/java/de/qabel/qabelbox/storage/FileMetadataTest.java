package de.qabel.qabelbox.storage;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import de.qabel.qabelbox.exceptions.QblStorageException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class FileMetadataTest {

	private BoxFile boxFile;
	private FileMetadata fileMetadata;

	@Before
	public void setUp() throws Exception {
		boxFile = new BoxFile("Block", "Name", 1000L, 1000L, new byte[]{0x00, 0x01, 0x02});
		fileMetadata = new FileMetadata(boxFile, new File(System.getProperty("java.io.tmpdir")));
	}

	@Test
	public void testFileMetadataBoxFile() throws QblStorageException {
		assertThat(fileMetadata.getFile(), is(equalTo(boxFile)));
	}

	@Test
	public void testRecreateFileMetadataFromFile() throws QblStorageException, IOException {
		FileMetadata fileMetadataFromTemp = new FileMetadata(fileMetadata.getPath());
		assertThat(fileMetadataFromTemp.getFile(), is(equalTo(boxFile)));
	}

	@Test
	public void testSpecVersion() throws QblStorageException {
		assertThat(fileMetadata.getSpecVersion(), is(0));
	}
}