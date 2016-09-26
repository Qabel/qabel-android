package de.qabel.qabelbox.box.interactor

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.*
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.chat.repository.ChatShareRepository
import de.qabel.chat.repository.entities.BoxFileChatShare
import de.qabel.chat.repository.entities.ShareStatus
import de.qabel.chat.repository.inmemory.InMemoryChatShareRepository
import de.qabel.chat.service.SharingService
import de.qabel.core.config.SymmetricKey
import de.qabel.core.extensions.assertThrows
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.ShareId
import de.qabel.qabelbox.box.provider.toDocumentId
import org.junit.Before
import org.junit.Test
import rx.lang.kotlin.toSingletonObservable
import java.util.*

class BoxDocumentIdAdapterTest {

    lateinit var useCase: BoxDocumentIdAdapter
    lateinit var fileBrowser: FileBrowser
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
        fileBrowser = mock()
        useCase = BoxDocumentIdAdapter(object : VolumeManager {
            override val roots: List<VolumeRoot>
                get() = volumes

            override fun fileBrowser(rootID: String) = fileBrowser
        }, shareRepo, mock(), sharingService)
    }

    @Test
    fun testAvailableRoots() {
        assertThat(useCase.availableRoots(), equalTo(volumes))
    }

    @Test
    fun testQueryChildDocuments() {
        whenever(fileBrowser.list(BoxPath.Root)).thenReturn(sampleFiles.toSingletonObservable())

        val lst = useCase.queryChildDocuments(docId).toBlocking().first()

        lst shouldMatch equalTo(listOf(
                ProviderEntry((volume.documentID + sample.name).toDocumentId(), sample)))
    }

    @Test
    fun testQuery() {
        whenever(fileBrowser.query(file)).thenReturn(sample.toSingletonObservable())

        val result = useCase.query(docId.copy(path = file)).toBlocking().first()
                as BrowserEntry.File

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
        val source = DownloadSource(sample, mock())
        whenever(fileBrowser.download(file)).thenReturn(source.toSingletonObservable())
        val download = useCase.download(docId.copy(path = file)).toBlocking().first()
        download.documentId shouldMatch equalTo(docId.copy(path = file))
        download.source.entry shouldMatch equalTo(sample)
        download.source.source shouldMatch equalTo(source.source)
    }

    @Test
    fun testUpload() {
        val source = UploadSource(mock(), sample)
        useCase.upload(ProviderUpload(docId.copy(path = file), source))
        verify(fileBrowser).upload(file, source)
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
