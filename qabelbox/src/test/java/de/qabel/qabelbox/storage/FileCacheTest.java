package de.qabel.qabelbox.storage;

import android.app.Application;
import android.support.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class FileCacheTest {

    private FileCache mHelper;
    private File testFile;
    private FileCache mCache;

    @Before
    public void setUp() throws Exception {
        Application application = RuntimeEnvironment.application;
        application.deleteDatabase(FileCache.DATABASE_NAME);
        mHelper = new FileCache(application);
        mCache = new FileCache(application);
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
