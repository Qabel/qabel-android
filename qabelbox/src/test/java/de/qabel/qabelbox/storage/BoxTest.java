package de.qabel.qabelbox.storage;


import android.content.Context;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.exceptions.QblStorageNameConflict;
import de.qabel.qabelbox.exceptions.QblStorageNotFound;
import de.qabel.qabelbox.util.TestHelper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class BoxTest {
    private static final Logger logger = LoggerFactory.getLogger(BoxTest.class.getName());
    private static final QblECPublicKey OWNER = new QblECKeyPair().getPub();

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
        new AppPreference(QabelBoxApplication.getInstance()).setToken(TestConstants.TOKEN);
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
    }

    private Context getContext() {
        return RuntimeEnvironment.application;
    }

    public static void fail(String message) {
        throw new AssertionError(message);
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
        volumeOtherUser.createIndex();


    }


    public BoxVolume getVolumeForRoot(Identity identity, byte[] deviceID, String prefix) {
        if (identity == null) {
            throw new NullPointerException("Identity is null");
        }
        QblECKeyPair key = identity.getPrimaryKeyPair();
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        return new BoxVolume(key, prefix,
                deviceID, getContext(), new FakeTransferManager(tmpDir));
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
        byte[] testData = new byte[]{1, 2, 3, 4, 5};
        outputStream.write(testData);
        outputStream.close();
        return file;
    }

    public void tearDown() throws IOException {
    }

    private void checkExternalReceivedBoxFile(byte[] originalFile, BoxFile boxFile, BoxNavigation navOtherUser) throws QblStorageException, IOException {
        List<BoxObject> boxExternalFiles = navOtherUser.listExternals();
        assertThat(boxExternalFiles.size(), is(1));
        assertThat(boxExternalFiles.get(0), instanceOf(BoxExternalFile.class));
        BoxExternalFile boxFileReceived = (BoxExternalFile) boxExternalFiles.get(0);
        assertThat(boxFile.name, is(equalTo(boxFileReceived.name)));
        assertThat(boxFile.key, is(equalTo(boxFileReceived.key)));
        assertThat(OWNER, is(equalTo(boxFileReceived.owner)));

        InputStream inputStream = navOtherUser.download(boxFileReceived, null);
        assertArrayEquals(originalFile, IOUtils.toByteArray(inputStream));
    }

    private void checkExternalReceivedBoxFile(BoxFile boxFile, BoxNavigation navOtherUser) throws QblStorageException {

        List<BoxObject> boxExternalFiles = navOtherUser.listExternals();
        assertThat(boxExternalFiles.size(), is(1));
        assertThat(boxExternalFiles.get(0), instanceOf(BoxExternalFile.class));
        BoxExternalFile boxFileReceived = (BoxExternalFile) boxExternalFiles.get(0);
        assertThat(boxFile.name, is(equalTo(boxFileReceived.name)));
        assertThat(boxFile.key, is(equalTo(boxFileReceived.key)));
        assertThat(OWNER, is(equalTo(boxFileReceived.owner)));

        InputStream inputStream = navOtherUser.download(boxFileReceived, null);

        //assertArrayEquals(originalFile, IOUtils.toByteArray(inputStream));
    }

    private BoxFile uploadFile(BoxNavigation nav, String name) throws QblStorageException, FileNotFoundException {
        File file = new File(testFileName);
        BoxFile boxFile = nav.upload(name, new FileInputStream(file), null);
        nav.commit();
        return boxFile;
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

        BoxExternalReference boxExternalReference = nav.createFileMetadata(OWNER, boxFile);

        // Share meta and metakey to other user

        BoxNavigation navOtherUser = volumeOtherUser.navigate();
        navOtherUser.attachExternal(boxExternalReference);

        //checkExternalReceivedBoxFile(IOUtils.toByteArray(new FileInputStream(file)), boxFile, navOtherUser);
    }

    @Test
    public void testShareAndUpdateFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        File file = new File(testFileName);
        BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
        nav.commit();

        BoxExternalReference boxExternalReference = nav.createFileMetadata(OWNER, boxFile);

        // Share meta and metakey to other user

        BoxNavigation navOtherUser = volumeOtherUser.navigate();
        navOtherUser.attachExternal(boxExternalReference);

        checkExternalReceivedBoxFile(boxFile, navOtherUser);

        boxFile = nav.upload("foobar", new FileInputStream(file), null);
        nav.commit();

        // Check that updated file can still be read

        checkExternalReceivedBoxFile(boxFile, navOtherUser);
    }

    @Test
    public void testShareAndRenameFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        File file = new File(testFileName);
        BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
        nav.commit();

        BoxExternalReference boxExternalReference = nav.createFileMetadata(OWNER, boxFile);

        // Share meta and metakey to other user

        BoxNavigation navOtherUser = volumeOtherUser.navigate();
        navOtherUser.attachExternal(boxExternalReference);

        checkExternalReceivedBoxFile(/*IOUtils.toByteArray(new FileInputStream(file)),*/ boxFile, navOtherUser);

        nav.rename(boxFile, "barfoo");
        nav.commit();

        // Check that updated file can still be read

        checkExternalReceivedBoxFile(IOUtils.toByteArray(new FileInputStream(file)), boxFile, navOtherUser);
    }

    @Test
    public void testShareAndUpdateAndUnshareFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxFile boxFile = uploadFile(nav, "foobar");

        BoxExternalReference boxExternalReference = nav.createFileMetadata(OWNER, boxFile);

        // Share meta and metakey to other user

        BoxNavigation navOtherUser = volumeOtherUser.navigate();
        navOtherUser.attachExternal(boxExternalReference);

        checkExternalReceivedBoxFile(boxFile, navOtherUser);

        boxFile = uploadFile(nav, "foobar");

        // Check that updated file can still be read

        //checkExternalReceivedBoxFile(IOUtils.toByteArray(new FileInputStream(new File(testFileName))), boxFile, navOtherUser);

        // Remove FileMetadata and update file
        nav.removeFileMetadata(boxFile);

        uploadFile(nav, "foobar");

        // Check that updated file cannot be read anymore
        List<BoxObject> boxExternalFiles = navOtherUser.listExternals();
        assertThat(boxExternalFiles.size(), is(0));
    }

    @Test
    public void testFileIsShared() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxFile boxFile = uploadFile(nav, "foobar");

        assertThat(boxFile.isShared(), is(false));

        nav.createFileMetadata(OWNER, boxFile);
        nav.commit();

        assertThat(boxFile.isShared(), is(true));

        nav.removeFileMetadata(boxFile);
        nav.commit();

        assertThat(boxFile.isShared(), is(false));

        // Check that BoxFile.meta and BoxFile.metakey is actually removed
        // from DirectoryMetadata and thus null in reloaded BoxFile.
        nav = volume.navigate();
        BoxFile receivedBoxFile = nav.listFiles().get(0);

        assertThat(boxFile, equalTo(receivedBoxFile));
        assertThat(false, is(boxFile.isShared()));

    }

    @Test
    public void testDetachFileMetadataShareFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxFile boxFile = uploadFile(nav, "foobar");

        BoxExternalReference boxExternalReference = nav.createFileMetadata(OWNER, boxFile);

        // Share meta and metakey to other user

        BoxNavigation navOtherUser = volumeOtherUser.navigate();
        navOtherUser.attachExternal(boxExternalReference);

        List<BoxObject> boxExternalFiles = navOtherUser.listExternals();
        assertThat(boxExternalFiles.size(), is(1));

        navOtherUser.detachExternal(boxExternalFiles.get(0).name);
        navOtherUser.commit();

        boxExternalFiles = navOtherUser.listExternals();
        assertThat(boxExternalFiles.size(), is(0));
    }

    @Test
    public void testDeleteFile() throws Exception {
        final BoxNavigation nav = volume.navigate();
        final BoxFile boxFile = uploadFile(nav);
        nav.delete(boxFile);
        nav.commit();
        TestHelper.waitUntil(() -> {
            try {
                nav.download(boxFile, null);
                return false;
            } catch (QblStorageNotFound e) {
                return true;
            }
        },
                "Expected QblStorageNotFound");
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
        assertThat("Download stream is null", dlStream, notNullValue());
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
        assertThat(nav.getPath(boxFolder), is("/foobdir/"));

        nav.navigate(boxFolder);
        BoxFile boxFile = uploadFile(nav);
        assertThat(nav.getPath(boxFile), is("/foobdir/foobar"));

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
        } catch (QblStorageNotFound e) {
        }
        try {
            nav.navigate(boxFolder);
            fail("Could navigate to deleted folder");
        } catch (QblStorageNotFound e) {
        }
        try {
            nav.navigate(subfolder);
            fail("Could navigate to deleted subfolder");
        } catch (QblStorageNotFound e) {
        }
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
        for (int i = 0; i < 1000; i++) {
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
        assertThat("Download stream is null", dlStream, notNullValue());
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
