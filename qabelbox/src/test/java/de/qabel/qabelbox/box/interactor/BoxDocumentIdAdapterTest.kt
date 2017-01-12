package de.qabel.qabelbox.box.interactor

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.*
import de.qabel.box.storage.dto.BoxPath
import de.qabel.chat.repository.ChatShareRepository
import de.qabel.chat.repository.entities.BoxFileChatShare
import de.qabel.chat.repository.entities.ShareStatus
import de.qabel.chat.repository.inmemory.InMemoryChatShareRepository
import de.qabel.chat.service.SharingService
import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.documentId.toDocumentId
import de.qabel.client.box.interactor.*
import de.qabel.core.config.SymmetricKey
import de.qabel.core.extensions.assertThrows
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.ShareId
import de.qabel.qabelbox.isEqual
import de.qabel.qabelbox.util.waitFor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rx.lang.kotlin.observable
import rx.lang.kotlin.toSingletonObservable
import java.io.File
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class BoxDocumentIdAdapterTest {

    lateinit var useCase: DocumentIdAdapter
    lateinit var readFileBrowser: ReadFileBrowser
    lateinit var operationFileBrowser: OperationFileBrowser
    val docId = DocumentId("identity", "prefix", BoxPath.Root)
    val volume = VolumeRoot("root", docId.toString(), "alias")
    val volumes = listOf(volume)
    val sample = BrowserEntry.File("foobar.txt", 42000, Date())
    val sampleFiles = listOf(sample)
    val file = BoxPath.Root * "foobar.txt"

    lateinit var shareRepo: ChatShareRepository
    lateinit var sharingService: SharingService

    @Before
    fun setUp() {
        shareRepo = InMemoryChatShareRepository()
        sharingService = mock()
        operationFileBrowser = mock()
        readFileBrowser = operationFileBrowser
        useCase = BoxDocumentIdAdapter(RuntimeEnvironment.application,
                object : VolumeManager {
                    override val roots: List<VolumeRoot>
                        get() = volumes

                    override fun readFileBrowser(rootID: String) = readFileBrowser
                    override fun operationFileBrowser(rootID: String) = operationFileBrowser
                }, shareRepo, mock(), sharingService)
    }

    @Test
    fun testAvailableRoots() {
        assertThat(useCase.availableRoots(), equalTo(volumes))
    }

    @Test
    fun testQueryChildDocuments() {
        whenever(readFileBrowser.list(BoxPath.Root)).thenReturn(sampleFiles.toSingletonObservable())

        val lst = useCase.queryChildDocuments(docId).toBlocking().first()

        lst shouldMatch equalTo(listOf(
                ProviderEntry((volume.documentID + sample.name).toDocumentId(), sample)))
    }

    @Test
    fun testQuery() {
        whenever(readFileBrowser.query(file)).thenReturn(sample.toSingletonObservable())

        val result = useCase.query(docId.copy(path = file)).waitFor() as BrowserEntry.File

        result shouldMatch equalTo(sample)
    }

    @Test
    fun testQueryChildFromFiles() {
        val lst = useCase.queryChildDocuments(docId.copy(path = file))
                .toBlocking().first()
        lst shouldMatch hasSize(equalTo(0))
    }

    @Test
    fun testDownload() {
        val operation = FileOperationState(BoxReadFileBrowser.KeyAndPrefix("", ""), file.name, file.parent)
        whenever(operationFileBrowser.download(eq(file), any())).then {
            return@then (Pair(operation, observable<FileOperationState> {
                it.onNext(operation)
                operation.status = FileOperationState.Status.COMPLETE
                it.onCompleted()
            }))
        }

        val (resultOperation, observable) = useCase.downloadFile(docId.copy(path = file), createTempFile())
        observable.waitFor()

        resultOperation.entryName isEqual (file.name)
        resultOperation.path isEqual file.parent
        resultOperation.status isEqual FileOperationState.Status.COMPLETE
    }

    @Test
    fun testUpload() {
        val inputFile: File = createTempFile()
        useCase.uploadFile(inputFile, docId.copy(path = file))
        verify(operationFileBrowser).upload(eq(file), any<UploadSource>())
    }

    @Test
    fun testDownloadShareFail() {
        val share = BoxFileChatShare(ShareStatus.NEW, "", 0L, SymmetricKey(emptyList()), "")
        shareRepo.persist(share)
        assertThrows(RuntimeException::class) {
            useCase.download(ShareId.create(share), mock()).toBlocking().value()
        }
    }

    @Test
    fun testDownloadShare() {
        val share = BoxFileChatShare(ShareStatus.ACCEPTED, "", 0L, SymmetricKey(emptyList()), "")
        share.prefix = "prefix"
        shareRepo.persist(share)
        useCase.download(ShareId.create(share), mock()).toBlocking().value()
        verify(sharingService).downloadShare(eq(share), any(), any())
    }

    @Test
    fun testRefreshShare() {
        val share = BoxFileChatShare(ShareStatus.ACCEPTED, "", 0L, SymmetricKey(emptyList()), "")
        share.prefix = "prefix"
        shareRepo.persist(share)
        useCase.refreshShare(ShareId.create(share)).toBlocking().value()
        verify(sharingService).updateShare(eq(share), any())
    }

}
