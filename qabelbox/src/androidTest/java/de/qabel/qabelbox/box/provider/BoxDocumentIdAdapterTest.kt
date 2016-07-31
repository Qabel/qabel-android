package de.qabel.qabelbox.box.provider

import android.provider.DocumentsContract
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.interactor.DocumentIdAdapter
import de.qabel.qabelbox.stubResult
import de.qabel.qabelbox.util.asString
import de.qabel.qabelbox.util.toByteArrayInputStream
import org.mockito.Mockito
import rx.lang.kotlin.toSingletonObservable
import java.util.*

class BoxDocumentIdAdapterTest : MockedBoxDocumentIdAdapterTest() {

    lateinit var useCase: DocumentIdAdapter

    val docId = DocumentId("identity", "prefix", BoxPath.Root)
    val volume = VolumeRoot("root", docId.toString(), "alias")
    val volumes = listOf(volume)
    val sample = BrowserEntry.File("foobar.txt", 42000, Date())
    val samplePayLoad = "foobar"

    override fun setUp() {
        super.setUp()
        useCase = Mockito.mock(DocumentIdAdapter::class.java)
        provider.injectProvider(useCase)
    }

    fun testQueryRoots() {
        stubResult(useCase.availableRoots(), volumes)
        val cursor = provider.queryRoots(BoxProvider.DEFAULT_ROOT_PROJECTION)
        cursor.count shouldMatch equalTo(1)
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
        val nameCol = query.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        query.getString(nameCol) shouldMatch equalTo("foobar.txt")
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

}

