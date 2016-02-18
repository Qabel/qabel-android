package de.qabel.qabelbox.storage;


import android.test.AndroidTestCase;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.exceptions.QblStorageNameConflict;
import de.qabel.qabelbox.exceptions.QblStorageNotFound;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class BoxTest extends AndroidTestCase {
    private static final String OWNER = "owner";

    Identity identity;
    Identity identityOtherUser;
    BoxVolume volume;
    BoxVolume volumeFromAnotherDevice;
    BoxVolume volumeOtherUser;
    byte[] deviceID;
    byte[] deviceID2;
	byte[] deviceIDOtherUser;
	QblECKeyPair keyPair;
	QblECKeyPair keyPairOtherUser;
    String prefix = "test"; // Don't touch at the moment the test-server only accepts this prefix in debug moder (using the magictoken)
    String prefixOtherUser = "test";
    private String testFileName;

    public void configureTestServer() {
        new AppPreference(QabelBoxApplication.getInstance().getApplicationContext()).setToken(QabelBoxApplication.getInstance().getApplicationContext().getString(R.string.blockserver_magic_testtoken));
    }

    @Before
    public void setUp() throws IOException, QblStorageException {
        configureTestServer();
        URI uri = URI.create(QabelBoxApplication.DEFAULT_DROP_SERVER);
        DropServer dropServer = new DropServer(uri, "", true);
        DropURL dropURL = new DropURL(dropServer);
        Collection<DropURL> dropURLs = new ArrayList<>();
        dropURLs.add(dropURL);

        CryptoUtils utils = new CryptoUtils();

        deviceID = utils.getRandomBytes(16);
        deviceID2 = utils.getRandomBytes(16);
		deviceIDOtherUser = utils.getRandomBytes(16);
        keyPair = new QblECKeyPair();
        keyPairOtherUser = new QblECKeyPair();

		testFileName = createTestFile();

        identity = new Identity("Default Test User", dropURLs, keyPair);
        identity.getPrefixes().add(prefix);
        identityOtherUser = new Identity("Second Test User", dropURLs, keyPairOtherUser);
        identityOtherUser.getPrefixes().add(prefixOtherUser);


        volume = getVolumeForRoot(identity, deviceID, prefix);
        volumeFromAnotherDevice = getVolumeForRoot(identity, deviceID2, prefix);
        volumeOtherUser = getVolumeForRoot(identityOtherUser, deviceIDOtherUser, prefixOtherUser);

        volume.createIndex();
        //volumeOtherUser.createIndex();


    }


    public BoxVolume getVolumeForRoot(Identity identity, byte[] deviceID, String prefix) {
        if (identity == null) {
            throw new NullPointerException("Identity is null");
        }
        QblECKeyPair key = identity.getPrimaryKeyPair();
        return new BoxVolume(key, prefix,
                deviceID, getContext());
    }

    public static String createTestFile() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File file = File.createTempFile("testfile", "test", tmpDir);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[1024];
        Arrays.fill(testData, (byte) 'f');
        for (int i = 0; i < 100; i++) {
            outputStream.write(testData);
        }
        outputStream.close();
        return file.getAbsolutePath();
    }

    public static File smallTestFile() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File file = File.createTempFile("testfile", "test", tmpDir);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[] {1,2,3,4,5};
		outputStream.write(testData);
        outputStream.close();
        return file;
    }

    public void tearDown() throws IOException {

    }

    @Test
    public void testCreateIndex() throws QblStorageException {
        BoxNavigation nav = volume.navigate();
        assertThat(nav.listFiles().size(), is(0));
    }

    @Test
    public void testUploadFile() throws QblStorageException, IOException {
        uploadFile(volume.navigate());
    }

    @Test
	public void testShareFile() throws QblStorageException, IOException {
		BoxNavigation nav = volume.navigate();
		File file = new File(testFileName);
		BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		nav.createFileMetadata(OWNER, boxFile);
		nav.commit();

		// Share meta and metakey to other user

		BoxNavigation navOtherUser = volumeOtherUser.navigate();
		navOtherUser.attachExternalFile(OWNER, boxFile.meta, boxFile.metakey);
		navOtherUser.commit();

		List<BoxExternalFile> boxExternalFiles = navOtherUser.listExternalFiles();
		assertThat(boxExternalFiles.size(), is(1));
		BoxExternalFile boxFileReceived = boxExternalFiles.get(0);
		assertThat(boxFile.block, is(equalTo(boxFileReceived.block)));
		assertThat(boxFile.name, is(equalTo(boxFileReceived.name)));
		assertThat(boxFile.size, is(equalTo(boxFileReceived.size)));
		assertThat(boxFile.mtime, is(equalTo(boxFileReceived.mtime)));
		assertThat(boxFile.key, is(equalTo(boxFileReceived.key)));
		assertThat(OWNER, is(equalTo(boxFileReceived.owner)));
	}

	@Test
	public void testFileIsShared() throws QblStorageException, IOException {
		BoxNavigation nav = volume.navigate();
		File file = new File(testFileName);
		BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		assertThat(boxFile.isShared(), is(false));

		nav.createFileMetadata(OWNER, boxFile);
		nav.commit();

		assertThat(boxFile.isShared(), is(true));

		nav.removeFileMetadata(boxFile);
		nav.commit();

		assertThat(boxFile.isShared(), is(false));
	}

	@Test
	public void testDetachFileMetadataShareFile() throws QblStorageException, IOException {
		BoxNavigation nav = volume.navigate();
		File file = new File(testFileName);
		BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		nav.createFileMetadata(OWNER, boxFile);
		nav.commit();

		// Share meta and metakey to other user

		BoxNavigation navOtherUser = volumeOtherUser.navigate();
		navOtherUser.attachExternalFile(OWNER, boxFile.meta, boxFile.metakey);
		navOtherUser.commit();

		List<BoxExternalFile> boxExternalFiles = navOtherUser.listExternalFiles();
		assertThat(boxExternalFiles.size(), is(1));

		navOtherUser.detachExternalFile(boxExternalFiles.get(0));
		navOtherUser.commit();

		boxExternalFiles = navOtherUser.listExternalFiles();
		assertThat(boxExternalFiles.size(), is(0));
	}

	@Test
    public void testDeleteFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxFile boxFile = uploadFile(nav);
        nav.delete(boxFile);
        nav.commit();
        try {
            nav.download(boxFile, null);
        } catch (QblStorageNotFound e) {
            return;
        }
        Assert.fail("Expected QblStorageNotFound");
    }

    private BoxFile uploadFile(BoxNavigation nav) throws QblStorageException, IOException {
        File file = new File(testFileName);
        long time = System.currentTimeMillis() / 1000;
        BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
        assertThat(boxFile.mtime, greaterThanOrEqualTo(time));
        assertThat(boxFile.size, not(equalTo(boxFile.mtime)));
        nav.commit();
        checkFile(boxFile, nav);
        return boxFile;
    }

    private void checkFile(BoxFile boxFile, BoxNavigation nav) throws QblStorageException, IOException {
        InputStream dlStream = nav.download(boxFile, null);
        assertNotNull("Download stream is null", dlStream);
        byte[] dl = IOUtils.toByteArray(dlStream);
        File file = new File(testFileName);
        byte[] content = IOUtils.toByteArray(new FileInputStream(file));
        assertThat(dl, is(content));
    }

    @Test
    public void testCreateFolder() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxFolder boxFolder = nav.createFolder("foobdir");
        nav.commit();
        assertThat(nav.getPath(boxFolder), is ("/foobdir/"));

        nav.navigate(boxFolder);
        BoxFile boxFile = uploadFile(nav);
        assertThat(nav.getPath(boxFile), is ("/foobdir/foobar"));

        checkFile(boxFile, nav);

        BoxNavigation nav_new = volume.navigate();
        List<BoxFolder> folders = nav_new.listFolders();
        assertThat(folders.size(), is(1));
        assertThat(boxFolder, equalTo(folders.get(0)));
    }

    @Test
    public void testDeleteFolder() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxFolder boxFolder = nav.createFolder("foobdir");
        nav.commit();

        nav.navigate(boxFolder);
        BoxFile boxFile = uploadFile(nav);
        BoxFolder subfolder = nav.createFolder("subfolder");
        nav.commit();

        nav = volume.navigate();
        nav.delete(boxFolder);
        nav.commit();
        BoxNavigation nav_after = volume.navigate();
        assertThat(nav_after.listFolders().isEmpty(), is(true));
        checkDeleted(boxFolder, subfolder, boxFile, nav_after);
    }

    private void checkDeleted(BoxFolder boxFolder, BoxFolder subfolder, BoxFile boxFile, BoxNavigation nav) throws QblStorageException {
        try {
            nav.download(boxFile, null);
            fail("Could download file in deleted folder");
        } catch (QblStorageNotFound e) { }
        try {
            nav.navigate(boxFolder);
            fail("Could navigate to deleted folder");
        } catch (QblStorageNotFound e) { }
        try {
            nav.navigate(subfolder);
            fail("Could navigate to deleted subfolder");
        } catch (QblStorageNotFound e) { }
    }

    @Test
    public void testOverrideFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        uploadFile(nav);
        uploadFile(nav);
        assertThat(nav.listFiles().size(), is(1));
    }

    @Test
    public void testConflictFileUpdate() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxNavigation nav2 = volumeFromAnotherDevice.navigate();
        File file = new File(testFileName);
        nav.upload("foobar", new FileInputStream(file), null);
        nav2.upload("foobar", new FileInputStream(file), null);
        nav2.commit();
        nav.commit();
        assertThat(nav.listFiles().size(), is(2));
    }

    @Test
    public void testFileNameConflict() throws QblStorageException, FileNotFoundException {
        BoxNavigation nav = volume.navigate();
        nav.createFolder("foobar");
        try {
            nav.upload("foobar", new FileInputStream(new File(testFileName)), null);
        } catch (QblStorageNameConflict e) {
            return;
        }
        fail("Expected QblStorageNameConflict");
    }

    @Test
    public void testFolderNameConflict() throws QblStorageException, FileNotFoundException {
        BoxNavigation nav = volume.navigate();
        nav.upload("foobar", new FileInputStream(new File(testFileName)), null);
        try {
            nav.createFolder("foobar");
        } catch (QblStorageNameConflict e) {
            return;
        }
        fail("Expected QblStorageNameConflict");
    }

    @Test
    public void testNavigateToIndirectSubfolder() throws QblStorageException {
        BoxNavigation nav = volume.navigate();
        BoxFolder boxFolder = nav.createFolder("foobdir");
        nav.commit();

        nav.navigate(boxFolder);
        BoxFolder subfolder = nav.createFolder("subfolder");
        nav.commit();

        nav = volume.navigate();
        try {
            nav.navigate(subfolder);
        } catch (QblStorageNotFound e) {
            return;
        }
        fail("Expected QblStorageNotFound");
    }

    @Test
    public void testNavigateToParent() throws QblStorageException {
        BoxNavigation nav = volume.navigate();
        BoxFolder boxFolder = nav.createFolder("foobdir");
        nav.commit();

        nav.navigate(boxFolder);
        BoxFolder subfolder = nav.createFolder("subfolder");
        nav.commit();

        nav.navigate(subfolder);
        BoxFolder subfolder2 = nav.createFolder("subfolder2");
        nav.commit();

        nav = volume.navigate();
        nav.navigate(boxFolder);
        nav.navigate(subfolder);
        nav.navigate(subfolder2);

        assertThat(nav.getPath(), is("/foobdir/subfolder/subfolder2/"));
        nav.navigateToParent();
        assertThat(nav.getPath(), is("/foobdir/subfolder/"));
        nav.navigateToParent();
        assertThat(nav.getPath(), is("/foobdir/"));
        nav.navigateToParent();
        assertThat(nav.getPath(), is("/"));
    }

    @Test
    public void testNavigateToParentOfRoot() throws QblStorageException {
        BoxNavigation nav = volume.navigate();

        try {
            nav.navigateToParent();
        } catch (QblStorageException e) {
            return;
        }
        fail("Expected QblStorageException");
    }

    @Test
    public void testNameConflictOnDifferentClients() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxNavigation nav2 = volumeFromAnotherDevice.navigate();
        File file = new File(testFileName);
        nav.upload("foobar", new FileInputStream(file), null);
        nav2.createFolder("foobar");
        nav2.commit();
        nav.commit();
        assertThat(nav.listFiles().size(), is(1));
        assertThat(nav.listFolders().size(), is(1));
        assertThat(nav.listFiles().get(0).name, startsWith("foobar_conflict"));
    }

    ///**
    // * Currently a folder with a name conflict just disappears and all is lost.
    // */
    //@Test
    //public void testFolderNameConflictOnDifferentClients() throws QblStorageException, IOException {
    //	BoxNavigation nav = volume.navigate();
    //	BoxNavigation nav2 = volumeFromAnotherDevice.navigate();
    //	File file = new File(testFileName);
    //	nav.createFolder("foobar");
    //	nav2.uploadAndDeleteLocalfile("foobar", new FileInputStream(file));
    //	nav2.commit();
    //	nav.commit();
    //	assertThat(nav.listFiles().size(), is(1));
    //	assertThat(nav.listFolders().size(), is(1));
    //	assertThat(nav.listFiles().get(0).name, startsWith("foobar_conflict"));
    //}

    @Test
    public void testUploadLargeFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        File file = File.createTempFile("large", "testfile");
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[1024];
        Arrays.fill(testData, (byte) 'f');
        for (int i=0; i<1000; i++) {
            outputStream.write(testData);
        }
        outputStream.close();
        nav.upload("large file", new FileInputStream(file), null);
    }

    @Test
    public void testCacheFailure() throws QblStorageException, IOException {
        File file = smallTestFile();
        BoxNavigation nav = volume.navigate();
        BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
        nav.commit();

        // warm up cache
        nav.download(boxFile, null);
        corruptCachedFile(boxFile);

        InputStream dlStream = nav.download(boxFile, null);
        assertNotNull("Download stream is null", dlStream);
        byte[] dl = IOUtils.toByteArray(dlStream);
        byte[] content = IOUtils.toByteArray(new FileInputStream(file));
        assertThat("Downloaded file is not correct", dl, is(content));
    }

    private void corruptCachedFile(BoxFile boxFile) throws IOException {
        // corrupt the file
        FileOutputStream outputStream = new FileOutputStream(new FileCache(getContext()).get(boxFile));
        outputStream.write(1);
        outputStream.close();
    }
}
