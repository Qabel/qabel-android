
package de.qabel.qabelbox.storage;

import android.util.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import de.qabel.qabelbox.test.TestConstants;
import de.qabel.qabelbox.test.files.FileHelper;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class SearchTestR {

    private static final String TAG = SearchTestR.class.getName();

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
    private static final String L0_FILE_1 = "Level0-one.bin";
    private static final String L0_DIR_1 = "dir1-level0-one";
    private static final String L1_FILE_1 = "Level1-one.bin";
    private static final String L1_FILE_2_SMALL = "Level1_two_small.bin";
    private static final String L1_DIR_1 = "dir1_level2-one";
    private static final String L2_FILE_1 = "One-level2-one.bin";
    private static final String L1_DIR_2 = "dir1-level2-two";
    private static final String L2_FILE_2 = "Two-level2-one.bin";

    private static final String[] fileNames = new String[]{L0_FILE_1, L1_FILE_1, L1_FILE_2_SMALL, L2_FILE_1, L2_FILE_2};
    private static final String[] folderNames = new String[]{L0_DIR_1, L1_DIR_1, L1_DIR_2};

    private static final int OBJECT_COUNT = fileNames.length + folderNames.length;

    private BoxNavigation navigation;

    @BeforeClass
    public void setUp() throws Exception {
        CryptoUtils utils = new CryptoUtils();
        byte[] deviceID = utils.getRandomBytes(16);
        QblECKeyPair keyPair = new QblECKeyPair();

        BoxVolume volume = new BoxVolume(keyPair, TestConstants.PREFIX,
                deviceID, RuntimeEnvironment.application);

        volume.createIndex();

        Log.d(TAG, "VOL :" + volume.toString());

        navigation = volume.navigate();

        setupFakeDirectoryStructure();

        Log.d(TAG, "SETUP DONE");
    }

    private void setupFakeDirectoryStructure() throws Exception {

        String testFile = FileHelper.createTestFile();
        String smallFile = FileHelper.smallTestFile().getAbsolutePath();

        assertThat(navigation.listFiles().size(), is(0));

        navigation.upload(L0_FILE_1, new FileInputStream(testFile), null);
        navigation.commit();

        BoxFolder folder = navigation.createFolder(L0_DIR_1);
        navigation.commit();
        navigation.navigate(folder);

        navigation.upload(L1_FILE_1, new FileInputStream(testFile), null);
        navigation.commit();
        navigation.upload(L1_FILE_2_SMALL, new FileInputStream(smallFile), null);
        navigation.commit();

        folder = navigation.createFolder(L1_DIR_1);
        navigation.commit();
        navigation.navigate(folder);

        navigation.upload(L2_FILE_1, new FileInputStream(testFile), null);
        navigation.commit();

        navigation.navigateToParent();

        folder = navigation.createFolder(L1_DIR_2);
        navigation.commit();
        navigation.navigate(folder);

        navigation.upload(L2_FILE_2, new FileInputStream(testFile), null);
        navigation.commit();

        debug(navigation);
    }

    @Before
    public void setup() throws QblStorageException {
        if (navigation.hasParent()) {
            navigation.navigateToRoot();
        }
    }

    private void debug(BoxNavigation nav) throws Exception {

        for (BoxFile file : nav.listFiles()) {
            Log.d(TAG, "FILE: " + file.name);
        }

        for (BoxFolder folder : nav.listFolders()) {
            Log.d(TAG, "DIR : " + folder.name);

            nav.navigate(folder);
            debug(nav);
            nav.navigateToParent();
        }
    }

    private void debug(BoxObject o) {
        if (o instanceof BoxFile) {
            BoxFile f = ((BoxFile) o);
            Log.d(TAG, "FILE: " + o.name + " @" + f.size + " D: " + f.mtime);
        } else {
            Log.d(TAG, "DIR : " + o.name);
        }
    }

    @Test
    public void testCollectAll() throws Exception {

        StorageSearch search = new StorageSearch(navigation);
        List<BoxObject> searchResults = search.getResults();

        for (BoxObject o : searchResults) {
            debug(o);
        }

        assertEquals(OBJECT_COUNT, searchResults.size());
    }

    @Test
    public void testForValidName() throws Exception {
        StorageSearch search = new StorageSearch(navigation);
        assertFalse(search.isValidSearchTerm(null));
        assertFalse(search.isValidSearchTerm(""));
        assertFalse(search.isValidSearchTerm(" "));

        assertTrue(search.isValidSearchTerm("1"));
    }

    @Test
    public void testNameSearch() throws Exception {
        List<BoxObject> lst = new StorageSearch(navigation).filterByName("small").getResults();

        assertEquals(1, lst.size());
        assertEquals(L1_FILE_2_SMALL, lst.get(0).name);

        StorageSearch search = new StorageSearch(navigation).filterByNameCaseSensitive("small");
        assertEquals(0, search.getResults().size());

        search = new StorageSearch(navigation).filterByNameCaseSensitive("Small");
        assertEquals(1, search.getResults().size());

        //if not valid don't apply the filter
        search = new StorageSearch(navigation).filterByName(null);
        assertEquals(8, search.getResults().size());

        search = new StorageSearch(navigation).filterByName("");
        assertEquals(8, search.getResults().size());

        search = new StorageSearch(navigation).filterByName(" ");
        assertEquals(8, search.getResults().size());
    }

    @Test
    public void testFilterBySize() throws Exception {

        StorageSearch search = new StorageSearch(navigation).filterByNameCaseSensitive("level1");
        assertEquals(3, search.getResults().size());

        search = new StorageSearch(navigation).filterByNameCaseSensitive("level1")
                .filterByMaximumSize(100);
        assertEquals(1, search.getResults().size());

        search = new StorageSearch(navigation).filterByNameCaseSensitive("level1")
                .filterByMaximumSize(110000);
        assertEquals(2, search.getResults().size());

        search = new StorageSearch(navigation).filterByNameCaseSensitive("level1")
                .filterByMaximumSize(100000);
        assertEquals(1, search.getResults().size());

        search = new StorageSearch(navigation).filterByNameCaseSensitive("level1")
                .filterByMinimumSize(1);
        assertEquals(2, search.getResults().size());

        search = new StorageSearch(navigation).filterByNameCaseSensitive("level1")
                .filterByMinimumSize(100);
        assertEquals(1, search.getResults().size());

        search = new StorageSearch(navigation).filterByNameCaseSensitive("level1")
                .filterByMinimumSize(10000000);
        assertEquals(0, search.getResults().size());
    }

    @Test
    public void testFilterByFileOrDir() throws Exception {

        StorageSearch search = new StorageSearch(navigation);
        List<BoxObject> objs = search.filterByNameCaseSensitive("level1")
                .filterOnlyDirectories().getResults();
        assertEquals(1, objs.size());

        List<BoxFolder> dirs = search.toBoxFolders(objs);
        assertEquals(1, dirs.size());
        assertEquals("dir1-level1-one", dirs.get(0).name);
        search.reset();

        objs = search.filterByNameCaseSensitive("level1").filterOnlyFiles().getResults();
        assertEquals(2, objs.size());

        List<BoxFile> files = search.toBoxFiles(objs);
        assertEquals(2, files.size());
        assertEquals(L1_FILE_1, files.get(0).name);
        assertEquals(L1_FILE_2_SMALL, files.get(1).name);
    }

    @Test
    public void testFilterByDate() throws Exception {

        StorageSearch search = new StorageSearch(navigation);
        List<BoxFile> files = search.toBoxFiles(search.getResults());
        assertEquals(fileNames.length, files.size());
        search.reset();

        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2010);

        files.get(0).mtime = calendar.getTimeInMillis();
        Date target = calendar.getTime();

        calendar.set(Calendar.YEAR, 2011);
        Date afterTarget = calendar.getTime();

        calendar.set(Calendar.YEAR, 2009);
        Date beforeTarget = calendar.getTime();

        search.filterByMinimumDate(afterTarget);
        assertEquals(5, search.getResults().size());
        search.reset();

        search.filterByMinimumDate(beforeTarget);
        assertEquals(5, search.getResults().size());
        search.reset();

        search = search.filterByMinimumDate(target);
        assertEquals(5, search.getResults().size());
        search.reset();

        search.filterByMaximumDate(target);
        assertEquals(0, search.getResults().size());
        search.reset();

        search.filterByMaximumDate(beforeTarget);
        assertEquals(0, search.getResults().size());

        search.filterByMaximumDate(afterTarget);
        assertEquals(0, search.getResults().size());
    }

    @Test
    public void testSortByName() throws Exception {

        StorageSearch search = new StorageSearch(navigation).sortCaseInsensitiveByName();
        assertEquals(L0_DIR_1, search.getResults().get(0).name);

        search.reset();
        search.sortCaseSensitiveByName();
        assertEquals(L2_FILE_1, search.getResults().get(0).name);
    }

    @Test
    public void testFilterByExtension() throws Exception {
        StorageSearch search = new StorageSearch(navigation);

        List<BoxObject> lst = search.filterByExtension("bin").getResults();
        assertEquals(5, lst.size());
        search.reset();

        lst = search.filterByExtension(".bin").getResults();
        assertEquals(5, lst.size());
        search.reset();

        lst = search.filterByExtension("BIN").getResults();
        assertEquals(5, lst.size());
        search.reset();

        lst = search.filterByExtension(".jpg").getResults();
        assertEquals(0, lst.size());
    }

    @Test
    public void testFindByPath() throws Exception {

        StorageSearch search = new StorageSearch(navigation);

        List<BoxObject> lst = search.filterByName("small").getResults();

        assertEquals(L1_FILE_2_SMALL, lst.get(0).name);

        assertEquals(lst.get(0), search.findByPath("/" + L0_DIR_1 + "/" + L1_FILE_2_SMALL));
    }

    @Test
    public void testFindPathByBoxObject() throws Exception {
        StorageSearch search = new StorageSearch(navigation);

        List<BoxObject> results = search.getResults();

        Log.d(TAG, "MAPS: " + search.getPathMapping().size());

        for (String key : search.getPathMapping().keySet()) {
            BoxObject o = search.getPathMapping().get(key);

            Log.d(TAG, "OBJ : " + o.name + " / " + o);
            Log.d(TAG, "PATH: " + key);
        }

        assertEquals(L0_FILE_1, results.get(0).name);
        assertEquals("/" + L0_FILE_1, search.findPathByBoxObject(results.get(0)));

        assertEquals(L0_DIR_1, results.get(1).name);
        assertEquals("/" + L0_DIR_1 + "/", search.findPathByBoxObject(results.get(1)));
    }
}

