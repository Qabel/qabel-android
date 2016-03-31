package de.qabel.qabelbox.storage;

import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.qabelbox.exceptions.QblStorageException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class FileMetadataTest {

    private static final QblECPublicKey OWNER = new QblECKeyPair().getPub();

    private BoxFile boxFile;
    private FileMetadata fileMetadata;

    @Before
    public void setUp() throws Exception {
        boxFile = new BoxFile("Prefix", "Block", "Name", 1000L, 1000L, new byte[]{0x00, 0x01, 0x02});
        fileMetadata = new FileMetadata(OWNER, boxFile, new File(System.getProperty("java.io.tmpdir")));
    }

    @Test
    public void testFileMetadataBoxFile() throws QblStorageException {
        BoxExternalFile boxFileFromMetadata = fileMetadata.getFile();
        assertThat(boxFile.block, is(equalTo(boxFileFromMetadata.block)));
        assertThat(boxFile.name, is(equalTo(boxFileFromMetadata.name)));
        assertThat(boxFile.size, is(equalTo(boxFileFromMetadata.size)));
        assertThat(boxFile.mtime, is(equalTo(boxFileFromMetadata.mtime)));
        assertThat(boxFile.key, is(equalTo(boxFileFromMetadata.key)));
    }

    @Test
    public void testRecreateFileMetadataFromFile() throws QblStorageException, IOException {
        FileMetadata fileMetadataFromTemp = new FileMetadata(fileMetadata.getPath());
        BoxExternalFile boxFileFromMetadata = fileMetadataFromTemp.getFile();
        assertThat(boxFile.block, is(equalTo(boxFileFromMetadata.block)));
        assertThat(boxFile.name, is(equalTo(boxFileFromMetadata.name)));
        assertThat(boxFile.size, is(equalTo(boxFileFromMetadata.size)));
        assertThat(boxFile.mtime, is(equalTo(boxFileFromMetadata.mtime)));
        assertThat(boxFile.key, is(equalTo(boxFileFromMetadata.key)));
        assertThat(OWNER, is(equalTo(boxFileFromMetadata.owner)));
    }

    @Test
    public void testSpecVersion() throws QblStorageException {
        assertThat(fileMetadata.getSpecVersion(), is(0));
    }
}
