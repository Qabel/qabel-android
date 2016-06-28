package de.qabel.qabelbox.providers

import android.annotation.TargetApi
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import de.qabel.box.storage.exceptions.QblStorageException

import org.apache.commons.io.IOUtils
import org.junit.Ignore

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.Arrays

import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.helper.MockedBoxProviderTest

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert

class BoxProviderTest : MockedBoxProviderTest() {

    private var testFileName: String? = null
    private var mContext: Context? = null

    override fun getContext(): Context {
        return mContext ?: throw IllegalStateException("No context")
    }

    @Throws(Exception::class)
    override fun setUp() {
        Log.d(TAG, "setUp")
        mContext = instrumentation.targetContext
        super.setUp()

        val tmpDir = File(System.getProperty("java.io.tmpdir"))
        val file = File.createTempFile("testfile", "test", tmpDir)
        val outputStream = FileOutputStream(file)
        val testData = ByteArray(1024)
        Arrays.fill(testData, 'f'.toByte())
        for (i in 0..99) {
            outputStream.write(testData)
        }
        outputStream.close()
        testFileName = file.absolutePath
    }


    @Throws(Exception::class)
    public override fun tearDown() {
        super.tearDown()
        Log.d(TAG, "tearDown")
    }

    @Throws(FileNotFoundException::class)
    fun testQueryRoots() {
        val cursor = provider.queryRoots(BoxProvider.DEFAULT_ROOT_PROJECTION)
        assertThat(cursor.count, `is`(1))
        cursor.moveToFirst()
        val documentId = cursor.getString(6)
        assertThat(documentId, startsWith(MockBoxProvider.PUB_KEY))

    }

    @Throws(IOException::class, QblStorageException::class)
    fun testOpenDocument() {
        val rootNav = volume.navigate()
        rootNav.upload("testfile", File(testFileName!!))
        rootNav.commit()
        assertThat(rootNav.listFiles().size, `is`(1))
        val testDocId = MockedBoxProviderTest.ROOT_DOC_ID + "testfile"
        val documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId)
        Assert.assertNotNull("Could not build document URI", documentUri)
        val query = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNotNull("Document query failed: " + documentUri.toString(), query)
        Assert.assertTrue(query!!.moveToFirst())
        val inputStream = mockContentResolver.openInputStream(documentUri)
        val dl = IOUtils.toByteArray(inputStream!!)
        val file = File(testFileName!!)
        val content = IOUtils.toByteArray(FileInputStream(file))
        assertThat(dl, `is`(content))
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun testCreateFile() {
        val testDocId = MockedBoxProviderTest.ROOT_DOC_ID + "testfile.png"
        val parentDocumentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, MockedBoxProviderTest.ROOT_DOC_ID)
        val documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId)
        Assert.assertNotNull("Could not build document URI", documentUri)
        var query: Cursor = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNull("Document already there: " + documentUri.toString(), query)
        val document = DocumentsContract.createDocument(mockContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png")
        Assert.assertNotNull("Create document failed, no document Uri returned", document)
        assertThat(document.toString(), `is`(documentUri.toString()))
        query = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNotNull("Document not created:" + documentUri.toString(), query)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun testDeleteFile() {
        val testDocId = MockedBoxProviderTest.ROOT_DOC_ID + "testfile.png"
        val parentDocumentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, MockedBoxProviderTest.ROOT_DOC_ID)
        val documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId)
        Assert.assertNotNull("Could not build document URI", documentUri)
        var query: Cursor = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNull("Document already there: " + documentUri.toString(), query)
        val document = DocumentsContract.createDocument(mockContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png")
        Assert.assertNotNull(document)
        DocumentsContract.deleteDocument(mockContentResolver, document)
        assertThat(document.toString(), `is`(documentUri.toString()))
        query = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNull("Document not deleted:" + documentUri.toString(), query)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun testRenameFile() {
        val testDocId = MockedBoxProviderTest.ROOT_DOC_ID + "testfile.png"
        val parentDocumentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, MockedBoxProviderTest.ROOT_DOC_ID)
        val documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId)
        Assert.assertNotNull("Could not build document URI", documentUri)
        var query: Cursor = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNull("Document already there: " + documentUri.toString(), query)
        val document = DocumentsContract.createDocument(
                mockContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png")
        Assert.assertNotNull(document)
        val renamed = DocumentsContract.renameDocument(mockContentResolver,
                document, "testfile2.png")
        Assert.assertNotNull(renamed)
        assertThat(renamed.toString(), `is`(parentDocumentUri.toString() + "testfile2.png"))
        query = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNull("Document not renamed:" + documentUri.toString(), query)
        query = mockContentResolver.query(renamed, null, null, null, null)
        Assert.assertNotNull("Document not renamed:" + documentUri.toString(), query)
    }

    /*
    @Throws(QblStorageException::class)
    fun testGetDocumentId() {
        assertThat(volume.getDocumentId("/"), `is`(MockedBoxProviderTest.ROOT_DOC_ID))
        val navigate = volume.navigate()
        assertThat(volume.getDocumentId(navigate.path), `is`(MockedBoxProviderTest.ROOT_DOC_ID))
        val folder = navigate.createFolder("testfolder")
        assertThat(navigate.getPath(folder), `is`("/testfolder/"))
        navigate.commit()
        navigate.navigate(folder)
        assertThat(volume.getDocumentId(navigate.path), `is`(MockedBoxProviderTest.ROOT_DOC_ID + "testfolder/"))
    }
    */

    companion object {

        private val TAG = "BoxProviderTest"
    }

}
