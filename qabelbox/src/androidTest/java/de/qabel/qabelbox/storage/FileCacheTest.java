package de.qabel.qabelbox.storage;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class FileCacheTest {

    private FileCache mHelper;
    private File testFile;
    private FileCache mCache;

    @Before
    public void setUp() throws Exception {
        getTargetContext().deleteDatabase(FileCache.DATABASE_NAME);
        mHelper = new FileCache(getTargetContext());
        mCache = new FileCache(getTargetContext());
        testFile = new File(BoxTest.createTestFile());
    }

    @After
    public void tearDown() throws Exception {
        mHelper.close();
    }

    @Test
    public void testHelperInsertFile() {
        BoxFile boxFile = getBoxFile();
        assertThat(mHelper.put(boxFile, testFile), equalTo(1L));
        assertThat(mHelper.get(boxFile), equalTo(testFile));
    }

    @NonNull
    private BoxFile getBoxFile() {
        Long now = System.currentTimeMillis() / 1000;
        return new BoxFile("prefix", "block", "name", 20L, now, null);
    }

    @Test
    public void testHelperCacheMiss() {
        assertNull(mHelper.get(getBoxFile()));
    }

    @Test
    public void testInsertFile() throws IOException {
        File file = new File(BoxTest.createTestFile());
        BoxFile boxFile = getBoxFile();
        mHelper.put(boxFile, file);
        assertThat(mHelper.get(boxFile), equalTo(file));
        // name change is ignored
        boxFile.name = "foobar";
        assertThat(mHelper.get(boxFile), equalTo(file));
        // mtime change not.
        boxFile.mtime += 1;
        assertNull(mHelper.get(boxFile));
        boxFile.mtime -= 1;
        // size change also not
        boxFile.size += 1;
        assertNull(mHelper.get(boxFile));
    }

    @Test
    public void testInvalidEntry() throws IOException {
        File file = new File(BoxTest.createTestFile());
        file.delete();
        BoxFile boxFile = getBoxFile();
        mHelper.put(boxFile, file);
        assertNull(mHelper.get(boxFile));
    }
}
