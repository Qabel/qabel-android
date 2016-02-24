
package de.qabel.qabelbox.storage;

import android.test.AndroidTestCase;
import android.util.Log;

import org.junit.Test;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


/**
 * Created by cdemon on 15.12.2015.
 */

public class SearchTest extends AndroidTestCase {

	private final static String TAG = SearchTest.class.getName();

	//will represent a filled resultset after setUp()
	//
	//level0-one.bin
	//+dir1-level1-one
	//  level1-ONE.bin
	//  level1-two-Small.bin
	//  +dir1-level2-one
	//      one-level2-one.bin
	//  +dir1-level2-two
	//      two-level2-one.bin
	//
	private static List<BoxObject> searchResults;
	private static Hashtable<String, BoxObject> pathMapping;

	private static boolean setup = true;

	public void setUp() throws Exception {
		if (!setup) {
			//setting up the directory structure takes time and so it is only made once - @AfterClass is not available here, so the cleanup is done, too
			return;
		}

		setup = false;

		final String prefix = "test";

		CryptoUtils utils = new CryptoUtils();
			byte[] deviceID = utils.getRandomBytes(16);
			QblECKeyPair keyPair = new QblECKeyPair();

		BoxVolume volume = new BoxVolume(keyPair, prefix,
					deviceID, getContext());

		volume.createIndex();

			Log.d(TAG, "VOL :" + volume.toString());

			BoxNavigation nav = volume.navigate();

			setupFakeDirectoryStructure(nav);

			setupBaseSearch(nav);


		Log.d(TAG, "SETUP DONE");
	}

	private void setupFakeDirectoryStructure(BoxNavigation nav) throws Exception {

		String testFile = BoxTest.createTestFile();
		String smallFile = BoxTest.smallTestFile().getAbsolutePath();

		assertThat(nav.listFiles().size(), is(0));

		nav.upload("level0-one.bin", new FileInputStream(testFile), null);
		nav.commit();

		BoxFolder folder = nav.createFolder("dir1-level1-one");
		nav.commit();
		nav.navigate(folder);

		nav.upload("level1-ONE.bin", new FileInputStream(testFile), null);
		nav.commit();
		nav.upload("level1-two-Small.bin", new FileInputStream(smallFile), null);
		nav.commit();

		folder = nav.createFolder("dir1-level2-one");
		nav.commit();
		nav.navigate(folder);

		nav.upload("one-level2-one.bin", new FileInputStream(testFile), null);
		nav.commit();

		nav.navigateToParent();

		folder = nav.createFolder("dir1-level2-two");
		nav.commit();
		nav.navigate(folder);

		nav.upload("two-level2-one.bin", new FileInputStream(testFile), null);
		nav.commit();

		while (nav.hasParent()) {
			nav.navigateToParent();
		}

		Log.d(TAG, "NAV : " + nav);

		debug(nav);
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

	private void setupBaseSearch(BoxNavigation nav) throws Exception {
		StorageSearch search = new StorageSearch(nav);

		searchResults = search.getResults();
		pathMapping = search.getPathMapping();
	}

	@Test
	public void testCollectAll() throws Exception {

		Log.d(TAG, "collectAll");

		for (BoxObject o : searchResults) {
			debug(o);
		}

		assertEquals(8, searchResults.size());

		Log.d(TAG, "/collectAll");
	}

	@Test
	public void testForValidName() throws Exception {
		assertFalse(StorageSearch.isValidSearchTerm(null));
		assertFalse(StorageSearch.isValidSearchTerm(""));
		assertFalse(StorageSearch.isValidSearchTerm(" "));

		assertTrue(StorageSearch.isValidSearchTerm("1"));
	}

	@Test
	public void testNameSearch() throws Exception {
		List<BoxObject> lst = new StorageSearch(searchResults).filterByName("small").getResults();

		assertEquals(1, lst.size());
		assertEquals("level1-two-Small.bin", lst.get(0).name);

		StorageSearch search = new StorageSearch(searchResults).filterByNameCaseSensitive("small");
		assertEquals(0, search.getResults().size());

		search = new StorageSearch(searchResults).filterByNameCaseSensitive("Small");
		assertEquals(1, search.getResults().size());

		//if not valid don't apply the filter
		search = new StorageSearch(searchResults).filterByName(null);
		assertEquals(8, search.getResults().size());

		search = new StorageSearch(searchResults).filterByName("");
		assertEquals(8, search.getResults().size());

		search = new StorageSearch(searchResults).filterByName(" ");
		assertEquals(8, search.getResults().size());
	}

	@Test
	public void testFilterBySize() throws Exception {

		StorageSearch search = new StorageSearch(searchResults).filterByNameCaseSensitive("level1");
		assertEquals(3, search.getResults().size());

		search = new StorageSearch(searchResults).filterByNameCaseSensitive("level1")
				.filterByMaximumSize(100);
		assertEquals(1, search.getResults().size());

		search = new StorageSearch(searchResults).filterByNameCaseSensitive("level1")
				.filterByMaximumSize(110000);
		assertEquals(2, search.getResults().size());

		search = new StorageSearch(searchResults).filterByNameCaseSensitive("level1")
				.filterByMaximumSize(100000);
		assertEquals(1, search.getResults().size());

		search = new StorageSearch(searchResults).filterByNameCaseSensitive("level1")
				.filterByMinimumSize(1);
		assertEquals(2, search.getResults().size());

		search = new StorageSearch(searchResults).filterByNameCaseSensitive("level1")
				.filterByMinimumSize(100);
		assertEquals(1, search.getResults().size());

		search = new StorageSearch(searchResults).filterByNameCaseSensitive("level1")
				.filterByMinimumSize(10000000);
		assertEquals(0, search.getResults().size());
	}

	@Test
	public void testFilterByFileOrDir() throws Exception {

		List<BoxObject> objs = new StorageSearch(searchResults).filterByNameCaseSensitive("level1")
				.filterOnlyDirectories().getResults();
		assertEquals(1, objs.size());

		List<BoxFolder> dirs = StorageSearch.toBoxFolders(objs);
		assertEquals(1, dirs.size());
		assertEquals("dir1-level1-one", dirs.get(0).name);

		objs = new StorageSearch(searchResults).filterByNameCaseSensitive("level1")
				.filterOnlyFiles().getResults();
		assertEquals(2, objs.size());

		List<BoxFile> files = StorageSearch.toBoxFiles(objs);
		assertEquals(2, files.size());
		assertEquals("level1-ONE.bin", files.get(0).name);
		assertEquals("level1-two-Small.bin", files.get(1).name);
	}

	@Test
	public void testFilterByDate() throws Exception {

		List<BoxFile> files = StorageSearch.toBoxFiles(searchResults);
		assertEquals(5, files.size());

		Calendar calendar = new GregorianCalendar();
		calendar.set(Calendar.YEAR, 2010);

		files.get(0).mtime = calendar.getTimeInMillis();
		Date target = calendar.getTime();

		calendar.set(Calendar.YEAR, 2011);
		Date afterTarget = calendar.getTime();

		calendar.set(Calendar.YEAR, 2009);
		Date beforeTarget = calendar.getTime();

		StorageSearch search = new StorageSearch(searchResults).filterByMinimumDate(afterTarget);
		assertEquals(5, search.getResults().size());

		search = new StorageSearch(searchResults).filterByMinimumDate(beforeTarget);
		assertEquals(5, search.getResults().size());

		search = new StorageSearch(searchResults).filterByMinimumDate(target);
		assertEquals(5, search.getResults().size());

		search = new StorageSearch(searchResults).filterByMaximumDate(target);
		assertEquals(0, search.getResults().size());

		search = new StorageSearch(searchResults).filterByMaximumDate(beforeTarget);
		assertEquals(0, search.getResults().size());

		search = new StorageSearch(searchResults).filterByMaximumDate(afterTarget);
		assertEquals(0, search.getResults().size());
	}

	@Test
	public void testSortByName() throws Exception {

		StorageSearch search = StorageSearch.createStorageSearchFromList(searchResults).sortCaseInsensitiveByName();
		assertEquals("dir1-level1-one", search.getResults().get(0).name);

		//we need to clone here since the test order is not guaranteed and other results depend on the original names
		List<BoxObject> clonedList = createClonedList(searchResults);

		search = new StorageSearch(clonedList).sortCaseSensitiveByName();
		assertEquals("LEVEL1-ONE.BIN", search.getResults().get(0).name);

		search = new StorageSearch(searchResults).sortCaseSensitiveByName();
		assertEquals("dir1-level1-one", search.getResults().get(0).name);
	}

	@Test
	public void testFilterByExtension() throws Exception {

		List<BoxObject> lst = new StorageSearch(searchResults).filterByExtension("bin").getResults();

		assertEquals(5, lst.size());

		lst = new StorageSearch(searchResults).filterByExtension(".bin").getResults();

		assertEquals(5, lst.size());

		lst = new StorageSearch(searchResults).filterByExtension("BIN").getResults();

		assertEquals(5, lst.size());

		lst = new StorageSearch(searchResults).filterByExtension(".jpg").getResults();

		assertEquals(0, lst.size());
	}

	@Test
	public void testFindByPath() throws Exception {

		StorageSearch search = new StorageSearch(searchResults, pathMapping);

		List<BoxObject> lst = search.filterByName("small").getResults();

		assertEquals("level1-two-Small.bin", lst.get(0).name);

		assertEquals(lst.get(0), search.findByPath("/dir1-level1-one/level1-two-Small.bin"));
	}

	@Test
	public void testFindPathByBoxObject() throws Exception {

		StorageSearch search = new StorageSearch(searchResults, pathMapping);

		List<BoxObject> results = search.getResults();

		Log.d(TAG, "MAPS: " + search.getPathMapping().size());

		for (String key : search.getPathMapping().keySet()) {
			BoxObject o = search.getPathMapping().get(key);

			Log.d(TAG, "OBJ : " + o.name + " / " + o);
			Log.d(TAG, "PATH: " + key);
		}

		assertEquals("level0-one.bin", results.get(0).name);
		assertEquals("/level0-one.bin", search.findPathByBoxObject(results.get(0)));

		assertEquals("dir1-level1-one", results.get(1).name);
		assertEquals("/dir1-level1-one/", search.findPathByBoxObject(results.get(1)));
	}

	private BoxObject clone(BoxObject o) {

		if (o instanceof BoxFile) {
			BoxFile tmp = (BoxFile) o;

			return new BoxFile(tmp.prefix, tmp.block, tmp.name, tmp.size, tmp.mtime, tmp.key);
		} else {
			BoxFolder tmp = (BoxFolder) o;

			return new BoxFolder(tmp.ref, tmp.name, tmp.key);
		}
	}

	private List<BoxObject> createClonedList(List<BoxObject> input) {

		List<BoxObject> clonedList = new ArrayList<>();

		for (BoxObject o : input) {
			clonedList.add(clone(o));
		}

		clonedList.get(2).name = clonedList.get(2).name.toUpperCase();

		return clonedList;
	}
}

