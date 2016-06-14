package de.qabel.qabelbox.storage;

import junit.framework.TestCase;

import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.model.BoxExternalReference;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxFolder;

import org.junit.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class DirectoryMetadataTest extends TestCase {

    private DirectoryMetadata dm;

    public void setUp() throws Exception {
        // device id
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        dm = DirectoryMetadata.newDatabase("https://localhost", bb.array(),
                new File(System.getProperty("java.io.tmpdir")));
    }

    @Test
    public void testInitDatabase() throws QblStorageException {
        byte[] version = dm.getVersion();
        assertThat(dm.listFiles().size(), is(0));
        assertThat(dm.listFolders().size(), is(0));
        assertThat(dm.listExternalReferences().size(), is(0));
        dm.commit();
        assertThat(dm.getVersion(), is(not(equalTo(version))));

    }

    @Test
    public void testFileOperations() throws QblStorageException {
        BoxFile file = new BoxFile("prefix", "block", "name", 0L, 0L, new byte[]{1, 2,}, "metablock", new byte[]{0x03, 0x04});
        dm.insertFile(file);
        assertThat(dm.listFiles().size(), is(1));
        assertThat(file, equalTo(dm.listFiles().get(0)));
        assertThat(file, is(dm.getFile("name")));
        dm.deleteFile(file);
        assertThat(dm.listFiles().size(), is(0));
    }

    @Test
    public void testFolderOperations() throws QblStorageException {
        BoxFolder folder = new BoxFolder("block", "name", new byte[]{1, 2,});
        dm.insertFolder(folder);
        assertThat(dm.listFolders().size(), is(1));
        assertThat(folder, equalTo(dm.listFolders().get(0)));
        dm.deleteFolder(folder);
        assertThat(dm.listFolders().size(), is(0));
    }

    @Test
    public void testExternalOperations() throws QblStorageException {
        BoxExternalReference external = new BoxExternalReference(false, "https://foobar", "name",
                new QblECKeyPair().getPub(), new byte[]{1, 2,});
        dm.insertExternalReference(external);
        assertThat(dm.listExternalReferences().size(), is(1));
        assertThat(external, equalTo(dm.listExternalReferences().get(0)));
        dm.deleteExternalReference(external.name);
        assertThat(dm.listExternalReferences().size(), is(0));
    }

    @Test
    public void testLastChangedBy() throws SQLException, QblStorageException {
        assertThat(dm.deviceId, is(dm.getLastChangedBy()));
        dm.deviceId = new byte[]{1, 1};
        dm.setLastChangedBy();
        assertThat(dm.deviceId, is(dm.getLastChangedBy()));
    }

    @Test
    public void testRoot() throws QblStorageException {
        assertThat(dm.getRoot(), startsWith("https://"));
    }

    @Test
    public void testSpecVersion() throws QblStorageException {
        assertThat(dm.getSpecVersion(), is(0));
    }
}
