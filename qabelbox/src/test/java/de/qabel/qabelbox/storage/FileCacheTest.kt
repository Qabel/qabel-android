package de.qabel.qabelbox.storage

import android.app.Application

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

import java.io.File
import java.io.IOException

import de.qabel.box.storage.BoxFile
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.test.files.FileHelper

import org.hamcrest.Matchers.*
import org.junit.Assert.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class FileCacheTest {

    private var mHelper: FileCache? = null
    private var testFile: File? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val application = RuntimeEnvironment.application
        application.deleteDatabase(FileCache.DATABASE_NAME)
        mHelper = FileCache(application)
        testFile = File(FileHelper.createTestFile())
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        mHelper!!.close()
    }

    @Test
    fun testHelperInsertFile() {
        val boxFile = boxFile
        assertThat(mHelper!!.put(boxFile, testFile), equalTo(1L))
        assertThat(mHelper!!.get(boxFile), equalTo<File>(testFile))
    }

    private val boxFile: BoxFile
        get() {
            val now = System.currentTimeMillis() / 1000
            return BoxFile("prefix", "block", "name", 20L, now, ByteArray(0))
        }

    @Test
    fun testHelperCacheMiss() {
        assertNull(mHelper!!.get(boxFile))
    }

    @Test
    @Throws(IOException::class)
    fun testInsertFile() {
        val file = File(FileHelper.createTestFile())
        val boxFile = boxFile
        mHelper!!.put(boxFile, file)
        assertThat(mHelper!!.get(boxFile), equalTo(file))
        // name change is ignored
        boxFile.name = "foobar"
        assertThat(mHelper!!.get(boxFile), equalTo(file))
        // mtime change not.
        boxFile.mtime = boxFile.mtime + 1
        assertNull(mHelper!!.get(boxFile))
        boxFile.mtime = boxFile.mtime - 1
        assertNull(mHelper!!.get(boxFile))
    }

    @Test
    @Throws(IOException::class)
    fun testInvalidEntry() {
        val file = File(FileHelper.createTestFile())
        file.delete()
        val boxFile = boxFile
        mHelper!!.put(boxFile, file)
        assertNull(mHelper!!.get(boxFile))
    }
}
