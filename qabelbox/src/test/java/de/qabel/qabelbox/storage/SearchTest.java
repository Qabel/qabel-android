
package de.qabel.qabelbox.storage;

import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.FileInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.model.BoxExternalReference;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxFolder;
import de.qabel.qabelbox.storage.model.BoxObject;
import de.qabel.qabelbox.storage.navigation.BoxNavigation;
import de.qabel.qabelbox.storage.transfer.FakeTransferManager;
import de.qabel.qabelbox.test.TestConstants;
import de.qabel.qabelbox.test.files.FileHelper;
import de.qabel.qabelbox.util.BoxTestHelper;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class SearchTest {

    private static final String TAG = SearchTest.class.getName();

    /**
     * Structure after setup()
     * <ul>
     * <li>level0-one.bin</li>
     * <li>+dir1-level0-one
     * <ul>
     * <li>level1-ONE.bin</li>
     * <li>level1-two-Small.bin</li>
     * <li>+dir1-level2-one
     * <ul>
     * <li>one-level2-one.bin</li>
     * </ul>
     * </li>
     * <li> +dir1-level2-two
     * <ul>
     * <li>two-level2-one.bin</li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * </ul>
     */
    private static final String L0_FILE_1 = "level0-one.bin";
    private static final String L0_DIR_1 = "dir1-level0-one";
    private static final String L1_FILE_1 = "level1-one.bin";
    private static final String L1_FILE_2_SMALL = "Level1_two_Small.bin";
    private static final String L1_DIR_1 = "dir1_level1-one";
    private static final String L2_FILE_1 = "One-level2-one.bin";
    private static final String L1_DIR_2 = "dir1-level2-two";
    private static final String L2_FILE_2 = "Two-level2-one.bin";
    private static final String SHARED_FILE_NAME = L0_FILE_1;

    private static final String[] fileNames = new String[]{L0_FILE_1, L1_FILE_1, L1_FILE_2_SMALL, L2_FILE_1, L2_FILE_2, SHARED_FILE_NAME};
    private static final String[] folderNames = new String[]{L0_DIR_1, L1_DIR_1, L1_DIR_2, BoxFolder.RECEIVED_SHARE_NAME};

    private static final int OBJECT_COUNT = fileNames.length + folderNames.length;

    private BoxNavigation navigation;
    private StorageSearch rootStorageSearch;

    @Before
    public void setUp() throws Exception {
        CryptoUtils utils = new CryptoUtils();
        byte[] deviceID = utils.getRandomBytes(16);
        QblECKeyPair keyPair = new QblECKeyPair();

        BoxTestHelper helper = new BoxTestHelper(RuntimeEnvironment.application);
        BoxManager manager = helper.createBoxManager();

        BoxVolume volume = new BoxVolume(keyPair, TestConstants.PREFIX,
                deviceID, RuntimeEnvironment.application,
                manager);

        volume.createIndex();

        Log.d(TAG, "VOL :" + volume.toString());

        navigation = volume.navigate();

        setupFakeDirectoryStructure(keyPair);

        navigation.navigateToRoot();
        rootStorageSearch = new StorageSearch(navigation);
    }

    private void setupFakeDirectoryStructure(QblECKeyPair keyPair) throws Exception {

        String testFile = FileHelper.createTestFile();
        String smallFile = FileHelper.smallTestFile().getAbsolutePath();

        assertThat(navigation.listFiles().size(), is(0));

        BoxFile sharedFile = navigation.upload(L0_FILE_1, new FileInputStream(testFile));
        BoxExternalReference externalReference = navigation.createFileMetadata(keyPair.getPub(), sharedFile);
        navigation.commit();

        BoxFolder folder = navigation.createFolder(L0_DIR_1);
        navigation.commit();
        navigation.navigate(folder);

        navigation.upload(L1_FILE_1, new FileInputStream(testFile));
        navigation.commit();
        navigation.upload(L1_FILE_2_SMALL, new FileInputStream(smallFile));
        navigation.commit();

        folder = navigation.createFolder(L1_DIR_1);
        navigation.commit();
        navigation.navigate(folder);

        navigation.upload(L2_FILE_1, new FileInputStream(testFile));
        navigation.commit();

        navigation.navigateToParent();

        folder = navigation.createFolder(L1_DIR_2);
        navigation.commit();
        navigation.navigate(folder);

        navigation.upload(L2_FILE_2, new FileInputStream(testFile));
        navigation.commit();

        //Add shares folder and shared file
        navigation.navigateToRoot();

        BoxFolder shares = navigation.createFolder(BoxFolder.RECEIVED_SHARE_NAME);
        navigation.commit();
        navigation.navigate(shares);

        //Attach file in shared folder
        navigation.attachExternal(externalReference);
        navigation.commit();
    }

    @Test
    public void testCollectAll() throws Exception {
        List<BoxObject> searchResults = rootStorageSearch.getResults();
        assertEquals(OBJECT_COUNT, searchResults.size());
    }

    @Test
    public void testForValidName() throws Exception {
        assertFalse(rootStorageSearch.isValidSearchTerm(null));
        assertFalse(rootStorageSearch.isValidSearchTerm(""));
        assertFalse(rootStorageSearch.isValidSearchTerm(" "));

        assertTrue(rootStorageSearch.isValidSearchTerm("1"));
    }

    @Test
    public void testNameSearch() throws Exception {
        List<BoxObject> lst = rootStorageSearch.filterByName("small").getResults();

        assertEquals(1, lst.size());
        assertEquals(L1_FILE_2_SMALL, lst.get(0).name);
        rootStorageSearch.reset();

        StorageSearch search = rootStorageSearch.filterByNameCaseSensitive("small");
        assertEquals(0, search.getResultSize());
        rootStorageSearch.reset();

        search = rootStorageSearch.filterByNameCaseSensitive("Small");
        assertEquals(1, search.getResultSize());
        rootStorageSearch.reset();

        search = rootStorageSearch.filterByName(null);
        assertEquals(OBJECT_COUNT, search.getResultSize());
        rootStorageSearch.reset();

        search = rootStorageSearch.filterByName("");
        assertEquals(OBJECT_COUNT, search.getResultSize());
        rootStorageSearch.reset();

        search = rootStorageSearch.filterByName(" ");
        assertEquals(OBJECT_COUNT, search.getResultSize());
    }

    @Test
    public void testFilterBySize() throws Exception {

        StorageSearch search = rootStorageSearch.filterByNameCaseInsensitive("level1");
        assertEquals(3, search.getResultSize());
        rootStorageSearch.reset();

        search = rootStorageSearch.filterByNameCaseInsensitive("level1")
                .filterByMaximumSize(100);
        assertEquals(1, search.getResultSize());
        rootStorageSearch.reset();

        search = rootStorageSearch.filterByNameCaseInsensitive("level1")
                .filterByMaximumSize(110000);
        assertEquals(2, search.getResultSize());
        rootStorageSearch.reset();

        search = rootStorageSearch.filterByNameCaseInsensitive("level1")
                .filterByMaximumSize(100000);
        assertEquals(1, search.getResultSize());
        rootStorageSearch.reset();

        search = rootStorageSearch.filterByNameCaseInsensitive("level1")
                .filterByMinimumSize(1);
        assertEquals(2, search.getResultSize());
        rootStorageSearch.reset();

        search = rootStorageSearch.filterByNameCaseInsensitive("level1")
                .filterByMinimumSize(100);
        assertEquals(1, search.getResultSize());
        rootStorageSearch.reset();

        search = rootStorageSearch.filterByNameCaseInsensitive("level1")
                .filterByMinimumSize(10000000);
        assertEquals(0, search.getResultSize());
    }

    @Test
    public void testFilterByFileOrDir() throws Exception {

        rootStorageSearch.filterByNameCaseSensitive("level1").filterOnlyDirectories();
        assertEquals(1, rootStorageSearch.getResultSize());

        List<BoxFolder> dirs = rootStorageSearch.toBoxFolders(rootStorageSearch.getResults());
        assertEquals(1, dirs.size());
        assertEquals(L1_DIR_1, dirs.get(0).name);
        rootStorageSearch.reset();

        rootStorageSearch.filterByNameCaseInsensitive("level1").filterOnlyFiles();
        assertEquals(2, rootStorageSearch.getResultSize());
        assertEquals(L1_FILE_1, rootStorageSearch.getResults().get(0).name);
        assertEquals(L1_FILE_2_SMALL, rootStorageSearch.getResults().get(1).name);
    }

    @Test
    public void testFilterByDate() throws Exception {

        List<BoxFile> files = rootStorageSearch.toBoxFiles(rootStorageSearch.getResults());
        assertEquals(fileNames.length, files.size());
        rootStorageSearch.reset();

        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2010);

        files.get(0).mtime = calendar.getTimeInMillis();
        Date target = calendar.getTime();

        calendar.set(Calendar.YEAR, 2011);
        Date afterTarget = calendar.getTime();

        calendar.set(Calendar.YEAR, 2009);
        Date beforeTarget = calendar.getTime();

        rootStorageSearch.filterByMinimumDate(afterTarget);
        assertEquals(6, rootStorageSearch.getResults().size());
        rootStorageSearch.reset();

        rootStorageSearch.filterByMinimumDate(beforeTarget);
        assertEquals(6, rootStorageSearch.getResults().size());
        rootStorageSearch.reset();

        rootStorageSearch = rootStorageSearch.filterByMinimumDate(target);
        assertEquals(6, rootStorageSearch.getResults().size());
        rootStorageSearch.reset();

        rootStorageSearch.filterByMaximumDate(target);
        assertEquals(0, rootStorageSearch.getResults().size());
        rootStorageSearch.reset();

        rootStorageSearch.filterByMaximumDate(beforeTarget);
        assertEquals(0, rootStorageSearch.getResults().size());
        rootStorageSearch.reset();

        rootStorageSearch.filterByMaximumDate(afterTarget);
        assertEquals(0, rootStorageSearch.getResults().size());
    }

    @Test
    public void testSortByName() throws Exception {
        rootStorageSearch.sortCaseInsensitiveByName();
        assertEquals(BoxFolder.RECEIVED_SHARE_NAME, rootStorageSearch.getResults().get(0).name);
        assertEquals(L0_DIR_1, rootStorageSearch.getResults().get(1).name);
        assertEquals(L2_FILE_2, rootStorageSearch.getResults().get(rootStorageSearch.getResults().size() - 1).name);

        rootStorageSearch.reset();
        rootStorageSearch.sortCaseSensitiveByName();
        assertEquals(L1_FILE_2_SMALL, rootStorageSearch.getResults().get(0).name);
        assertEquals(L1_FILE_1, rootStorageSearch.getResults().get(rootStorageSearch.getResults().size() - 1).name);
    }

    @Test
    public void testFilterByExtension() throws Exception {
        List<BoxObject> lst = rootStorageSearch.filterByExtension("bin").getResults();

        assertEquals(6, lst.size());
        rootStorageSearch.reset();

        lst = rootStorageSearch.filterByExtension(".bin").getResults();
        assertEquals(6, lst.size());
        rootStorageSearch.reset();

        lst = rootStorageSearch.filterByExtension("BIN").getResults();
        assertEquals(6, lst.size());
        rootStorageSearch.reset();

        lst = rootStorageSearch.filterByExtension(".jpg").getResults();
        assertEquals(0, lst.size());
    }

    @Test
    public void testFindByPath() throws Exception {
        List<BoxObject> lst = rootStorageSearch.filterByName("small").getResults();

        assertEquals(L1_FILE_2_SMALL, lst.get(0).name);
        assertEquals(lst.get(0).name, rootStorageSearch.findByPath("/" + L0_DIR_1 + "/" + L1_FILE_2_SMALL).name);
    }

    @Test
    public void testFindPathByBoxObject() throws Exception {
        List<BoxObject> results = rootStorageSearch.getResults();

        assertEquals(L0_FILE_1, results.get(0).name);
        assertEquals("/" + L0_FILE_1, rootStorageSearch.findPathByBoxObject(results.get(0)));

        assertEquals(L0_DIR_1, results.get(1).name);
        assertEquals("/" + L0_DIR_1 + "/", rootStorageSearch.findPathByBoxObject(results.get(1)));
    }

    private BoxFolder navigateToFolder(String name) throws QblStorageException {
        List<BoxFolder> folders = navigation.listFolders();
        BoxFolder targetSubfolder = null;
        for (BoxFolder folder : folders) {
            if (folder.name.equals(name)) {
                targetSubfolder = folder;
                break;
            }
        }
        if (targetSubfolder == null) {
            throw new QblStorageException(String.format("Folder %s not found!", name));
        }
        navigation.navigate(targetSubfolder);
        return targetSubfolder;
    }

    @Test
    public void testSubFolderSearch() throws Exception {
        navigateToFolder(L0_DIR_1);
        navigateToFolder(L1_DIR_1);
        assertTrue(navigation.hasParent());
        StorageSearch subfolderSearch = new StorageSearch(navigation);
        assertEquals(1, subfolderSearch.getResultSize());

        subfolderSearch.filterOnlyFiles();
        assertEquals(1, subfolderSearch.getResultSize());
        subfolderSearch.reset();

        subfolderSearch.filterOnlyDirectories();
        assertEquals(0, subfolderSearch.getResultSize());

        navigation.navigateToParent();
        subfolderSearch.refreshRange(navigation);

        assertEquals(6, subfolderSearch.getResultSize());
        subfolderSearch.filterOnlyDirectories();
        assertEquals(2, subfolderSearch.getResultSize());
    }

    @Test
    public void testShareFolder() throws Exception {
        rootStorageSearch.filterByName(BoxFolder.RECEIVED_SHARE_NAME.replace("[", "").replace("]", "")).filterOnlyDirectories();
        assertEquals(1, rootStorageSearch.getResultSize());
        rootStorageSearch.reset();

        rootStorageSearch.filterByName(SHARED_FILE_NAME).filterOnlyFiles();
        assertEquals(2, rootStorageSearch.getResultSize());

        navigateToFolder(BoxFolder.RECEIVED_SHARE_NAME);
        StorageSearch sharedSearch = new StorageSearch(navigation);
        assertEquals(1, sharedSearch.getResultSize());
        sharedSearch.filterByName(L1_FILE_1);
        assertEquals(0, sharedSearch.getResultSize());
        sharedSearch.reset();

        assertEquals(SHARED_FILE_NAME, sharedSearch.getResults().get(0).name);
    }
}

