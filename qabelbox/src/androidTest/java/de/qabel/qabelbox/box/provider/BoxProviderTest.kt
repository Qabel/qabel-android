package de.qabel.qabelbox.box.provider

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.interactor.*
import de.qabel.qabelbox.stubResult
import de.qabel.qabelbox.util.asString
import de.qabel.qabelbox.util.toByteArrayInputStream
import junit.framework.Assert
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.mockito.Mockito
import rx.lang.kotlin.toSingletonObservable
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*

class BoxProviderTest : MockedBoxProviderTest() {
    override val context: Context
        get() = instrumentation.targetContext

    lateinit var testFileName: String
    lateinit var useCase: ProviderUseCase

    val docId = DocumentId("identity", "prefix", BoxPath.Root)
    val volume = VolumeRoot("root", docId.toString(), "alias")
    val volumes = listOf(volume)
    val sample = BrowserEntry.File("foobar.txt", 42000, Date())
    val samplePayLoad = "foobar"

    override fun setUp() {
        super.setUp()
        useCase = Mockito.mock(ProviderUseCase::class.java)
        provider.injectProvider(useCase)

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

    fun testQueryRoots() {
        stubResult(useCase.availableRoots(), volumes)
        val cursor = provider.queryRoots(BoxProvider.DEFAULT_ROOT_PROJECTION)
        assertThat(cursor.count, `is`(1))
        cursor.moveToFirst()
        val documentId = cursor.getString(6)
        documentId shouldMatch equalTo(volume.documentID)
    }

    fun testQueryDocument() {
        val document = docId.copy(path = BoxPath.Root * "foobar.txt")
        stubResult(useCase.query(document), sample.toSingletonObservable())
        val query = provider.queryDocument(document.toString(), null)
                ?: throw AssertionError("cursor null")
        query.moveToFirst()
        val idCol = query.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        query.getString(idCol) shouldMatch equalTo(document.toString())
    }

    fun testQueryChildDocuments() {
        val document = docId.copy(path = BoxPath.Root)
        val sampleId = document.copy(path = BoxPath.Root * sample.name)
        val sampleFolder = document.copy(path = BoxPath.Root / "folder")
        val listing = listOf(ProviderEntry(sampleId, sample),
                             ProviderEntry(sampleFolder, BrowserEntry.Folder("folder")))
        stubResult(useCase.queryChildDocuments(document), listing.toSingletonObservable())

        val query = provider.queryChildDocuments(document.toString(), null, null)

        query.count shouldMatch equalTo(2)
        query.moveToFirst()
        val idCol = query.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        query.getString(idCol) shouldMatch equalTo(sampleId.toString())
        query.moveToNext()
        query.getString(idCol) shouldMatch equalTo(sampleFolder.toString())
    }

    fun testOpenDocument() {
        val document = docId.copy(path = BoxPath.Root * "foobar.txt")
        stubResult(useCase.query(document), sample.toSingletonObservable())
        stubResult(useCase.download(document),
                ProviderDownload(document,
                    DownloadSource(sample,
                    samplePayLoad.toByteArrayInputStream())).toSingletonObservable())
        val documentUri = DocumentsContract.buildDocumentUri(
                BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, document.toString())
        mockContentResolver.query(documentUri, null, null, null, null).use {
            assertTrue("No result for query", it.moveToFirst())
            val inputStream = mockContentResolver.openInputStream(documentUri)
            inputStream.asString() shouldMatch equalTo(samplePayLoad)
        }
    }

    /*
    fun testOpenDocumentWritable() {
        val browser = MockFileBrowserUseCase()
        val useCase = BoxProviderUseCase(object: VolumeManager {
            override val roots: List<VolumeRoot>
                get() = throw UnsupportedOperationException()

            override fun fileBrowser(rootID: String) = browser

        })
        provider.injectProvider(useCase)
        val file = BoxPath.Root * "foobar.txt"
        val document = docId.copy(path = file)
        val documentUri = DocumentsContract.buildDocumentUri(
                BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, document.toString())
        provider.handlerThread.start()

        val fd = mockContentResolver.openFileDescriptor(documentUri, "w")
        val out = ParcelFileDescriptor.AutoCloseOutputStream(fd)
        out.write(samplePayLoad.toByteArray())
        out.close()
        while (!provider.handlerThread.looper.queue.isIdle) {
            Thread.sleep(50)
        }
        browser.download(file).toBlocking().first().source.asString() shouldMatch equalTo(samplePayLoad)
    }
    */

}

