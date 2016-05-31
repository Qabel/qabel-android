package de.qabel.qabelbox.storage;


import android.content.Context;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

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
import de.qabel.qabelbox.storage.model.BoxExternalFile;
import de.qabel.qabelbox.storage.model.BoxExternalReference;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxFolder;
import de.qabel.qabelbox.storage.model.BoxObject;
import de.qabel.qabelbox.storage.navigation.BoxNavigation;
import de.qabel.qabelbox.storage.transfer.FakeTransferManager;
import de.qabel.qabelbox.test.files.FileHelper;
import de.qabel.qabelbox.util.BoxTestHelper;
import de.qabel.qabelbox.util.TestHelper;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class BoxTest {

    private static final QblECPublicKey OWNER = new QblECKeyPair().getPub();
    private static final String SHARED_FILE_NAME = "sharedFile";

    private BoxManager boxManager;

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
    private String testFilePath;

    private BoxExternalReference sharedReference;
    private BoxFile sharedFile;

    public void configureTestServer() {
        new AppPreference(QabelBoxApplication.getInstance()).setToken(TestConstants.TOKEN);
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
    }

    private Context getContext() {
        return RuntimeEnvironment.application;
    }

    @Before
    public void setUp() throws IOException, QblStorageException {
        configureTestServer();
        boxManager = BoxTestHelper.createBoxManager(RuntimeEnvironment.application);

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

        testFilePath = FileHelper.createTestFile();

        identity = new Identity("Default Test User", dropURLs, keyPair);
        identity.getPrefixes().add(prefix);
        identityOtherUser = new Identity("Second Test User", dropURLs, keyPairOtherUser);
        identityOtherUser.getPrefixes().add(prefixOtherUser);


        volume = getVolumeForRoot(identity, deviceID, prefix);
        volumeFromAnotherDevice = getVolumeForRoot(identity, deviceID2, prefix);
        volumeOtherUser = getVolumeForRoot(identityOtherUser, deviceIDOtherUser, prefixOtherUser);

        volume.createIndex();
        volumeOtherUser.createIndex();

        BoxNavigation nav = volume.navigate();
        File file = FileHelper.smallTestFile();
        sharedFile = nav.upload(SHARED_FILE_NAME, new FileInputStream(file));
        sharedReference = nav.createFileMetadata(OWNER, sharedFile);
        nav.commit();

        // Share meta and metakey to other user
        BoxNavigation navOtherUser = volumeOtherUser.navigate();
        navOtherUser.attachExternal(sharedReference);
        navOtherUser.commit();
    }


    public BoxVolume getVolumeForRoot(Identity identity, byte[] deviceID, String prefix) {
        if (identity == null) {
            throw new NullPointerException("Identity is null");
        }
        QblECKeyPair key = identity.getPrimaryKeyPair();
        return new BoxVolume(key, prefix,
                deviceID, getContext(), boxManager);
    }

    public void tearDown() throws IOException {
    }

    private <T extends BoxObject> T findByName(String name, List<T> boxObjects) {
        T boxObject = null;
        for (T object : boxObjects) {
            if (object.name.equals(name)) {
                boxObject = object;
            }
        }
        return boxObject;
    }

    private void checkExternalReceivedBoxFile(byte[] originalFile, BoxFile boxFile, BoxNavigation navOtherUser) throws QblStorageException, IOException {

        BoxObject sharedObject = findByName(boxFile.name, navOtherUser.listExternals());
        assertNotNull(sharedObject);

        assertThat(sharedObject, instanceOf(BoxExternalFile.class));
        BoxExternalFile boxFileReceived = (BoxExternalFile) sharedObject;

        assertThat(boxFile.name, is(equalTo(boxFileReceived.name)));
        assertThat(boxFile.key, is(equalTo(boxFileReceived.key)));
        assertThat(OWNER, is(equalTo(boxFileReceived.owner)));

        InputStream inputStream = navOtherUser.download(boxFileReceived);
        assertArrayEquals(originalFile, IOUtils.toByteArray(inputStream));
    }

    private BoxFile uploadFile(BoxNavigation nav, String name) throws QblStorageException, FileNotFoundException {
        File file = new File(testFilePath);
        BoxFile boxFile = nav.upload(name, new FileInputStream(file));
        nav.commit();
        return boxFile;
    }

    @Test
    public void testCreateIndex() throws QblStorageException {
        BoxNavigation nav = volume.navigate();
        assertThat(nav.listFiles().size(), is(1));
    }

    @Test
    public void testUploadFile() throws QblStorageException, IOException {
        uploadFile(volume.navigate());
    }

    @Test
    public void testShareFile() throws QblStorageException, IOException {
        String filename = "foobar";
        //Create
        BoxNavigation nav = volume.navigate();
        File file = FileHelper.smallTestFile();
        BoxFile boxFile = nav.upload(filename, new FileInputStream(file));

        //Create external
        BoxExternalReference boxExternalReference = nav.createFileMetadata(OWNER, boxFile);
        nav.commit();

        // Share meta and metakey to other user
        BoxNavigation navOtherUser = volumeOtherUser.navigate();
        navOtherUser.attachExternal(boxExternalReference);
        navOtherUser.commit();

        checkExternalReceivedBoxFile(IOUtils.toByteArray(new FileInputStream(file)), boxFile, navOtherUser);

        assertEquals(2, navOtherUser.listExternals().size());
    }

    @Test
    public void testUpdateSharedFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();

        BoxFile updatedFile = nav.upload(SHARED_FILE_NAME, new FileInputStream(testFilePath));
        nav.commit();

        BoxNavigation otherNav = volumeOtherUser.navigate();

        // Check that updated file
        byte[] localData = IOUtils.toByteArray(nav.download(updatedFile));
        checkExternalReceivedBoxFile(localData, updatedFile, otherNav);
    }

    @Test
    public void testRenameSharedFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        nav.rename(sharedFile, "barfoo");
        nav.commit();

        // Check that updated file can still be read
        byte[] originalData = IOUtils.toByteArray(nav.download(sharedFile));
        checkExternalReceivedBoxFile(originalData, sharedFile, volumeOtherUser.navigate());
    }

    @Test
    public void testUnshareFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();

        // Remove FileMetadata and update file
        nav.removeFileMetadata(sharedFile);

        // Check that updated file cannot be read anymore
        BoxNavigation navOtherUser = volumeOtherUser.navigate();
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
        BoxFile receivedBoxFile = findByName(boxFile.name, nav.listFiles());

        assertThat(boxFile, equalTo(receivedBoxFile));
        assertThat(false, is(boxFile.isShared()));

    }

    @Test
    public void testDetachSharedFile() throws QblStorageException, IOException {
        // Share meta and metakey to other user
        BoxNavigation navOtherUser = volumeOtherUser.navigate();
        navOtherUser.detachExternal(sharedFile.name);
        navOtherUser.commit();

        assertEquals(0, navOtherUser.listExternals().size());
    }

    @Test
    public void testDeleteFile() throws Exception {
        final BoxNavigation nav = volume.navigate();
        final BoxFile boxFile = uploadFile(nav);
        nav.delete(boxFile);
        nav.commit();
        TestHelper.waitUntil(() -> {
            try {
                nav.download(boxFile);
                return false;
            } catch (QblStorageNotFound e) {
                return true;
            }
        }, "Expected QblStorageNotFound");
    }

    private BoxFile uploadFile(BoxNavigation nav) throws QblStorageException, IOException {
        File file = new File(testFilePath);
        long time = System.currentTimeMillis() / 1000;
        BoxFile boxFile = nav.upload("foobar", new FileInputStream(file));
        assertThat(boxFile.mtime, greaterThanOrEqualTo(time));
        assertThat(boxFile.size, not(equalTo(boxFile.mtime)));
        nav.commit();
        checkFile(boxFile, nav);
        return boxFile;
    }

    private void checkFile(BoxFile boxFile, BoxNavigation nav) throws QblStorageException, IOException {
        InputStream dlStream = nav.download(boxFile);
        assertThat("Download stream is null", dlStream, notNullValue());
        byte[] dl = IOUtils.toByteArray(dlStream);
        File file = new File(testFilePath);
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
            nav.download(boxFile);
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
        assertThat(nav.listFiles().size(), is(2));
    }

    @Test
    public void testConflictFileUpdate() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxNavigation nav2 = volumeFromAnotherDevice.navigate();
        File file = new File(testFilePath);
        nav.upload("foobar", new FileInputStream(file));
        nav2.upload("foobar", new FileInputStream(file));
        nav2.commit();
        nav.commit();
        assertThat(nav.listFiles().size(), is(3));
    }

    @Test
    public void testFileNameConflict() throws QblStorageException, FileNotFoundException {
        BoxNavigation nav = volume.navigate();
        nav.createFolder("foobar");
        try {
            nav.upload("foobar", new FileInputStream(new File(testFilePath)));
        } catch (QblStorageNameConflict e) {
            return;
        }
        fail("Expected QblStorageNameConflict");
    }

    @Test
    public void testFolderNameConflict() throws QblStorageException, FileNotFoundException {
        BoxNavigation nav = volume.navigate();
        nav.upload("foobar", new FileInputStream(new File(testFilePath)));
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
        File file = new File(testFilePath);
        nav.upload("foobar", new FileInputStream(file));
        nav2.createFolder("foobar");
        nav2.commit();
        nav.commit();

        assertThat(nav.listFiles().size(), is(2));
        assertThat(nav.listFolders().size(), is(1));
        StorageSearch storageSearch = new StorageSearch(nav).filterByName("foobar");
        //Folder and file
        assertEquals(2, storageSearch.getResultSize());
        //Check file name
        storageSearch.filterOnlyFiles();
        assertEquals(1, storageSearch.getResultSize());
        assertThat(storageSearch.getResults().get(0).name, startsWith("foobar_conflict"));
    }

    ///**
    // * Currently a folder with a name conflict just disappears and all is lost.
    // */
    //@Test
    //public void testFolderNameConflictOnDifferentClients() throws QblStorageException, IOException {
    //	BoxNavigation nav = volume.navigate();
    //	BoxNavigation nav2 = volumeFromAnotherDevice.navigate();
    //	File file = new File(testFilePath);
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
        nav.upload("large file", new FileInputStream(file));
    }

    @Test
    public void testCacheFailure() throws QblStorageException, IOException {
        File file = FileHelper.smallTestFile();
        BoxNavigation nav = volume.navigate();
        BoxFile boxFile = nav.upload("foobar", new FileInputStream(file));
        nav.commit();

        // warm up cache
        nav.download(boxFile);
        corruptCachedFile(boxFile);

        InputStream dlStream = nav.download(boxFile);
        assertThat("Download stream is null", dlStream, notNullValue());
        byte[] dl = IOUtils.toByteArray(dlStream);
        byte[] content = IOUtils.toByteArray(new FileInputStream(file));
        assertThat("Downloaded file is not correct", dl, is(content));
    }

    private void corruptCachedFile(BoxFile boxFile) throws IOException {
        // corrupt the file
        FileOutputStream outputStream = new FileOutputStream(
                new FileCache(getContext()).get(boxFile));
        outputStream.write(1);
        outputStream.close();
    }
}
