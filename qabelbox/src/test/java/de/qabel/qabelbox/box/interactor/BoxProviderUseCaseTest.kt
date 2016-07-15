package de.qabel.qabelbox.box.interactor

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.ProviderEntry
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.toDocumentId
import org.junit.Before
import org.junit.Test
import rx.lang.kotlin.toSingletonObservable
import java.util.*

class BoxProviderUseCaseTest {

    lateinit var useCase: BoxProviderUseCase
    lateinit var fileBrowser: FileBrowserUseCase
    val docId = DocumentId("identity", "prefix", BoxPath.Root)
    val volume = VolumeRoot("root", docId.toString(), "alias")
    val volumes = listOf(volume)
    val sample = BrowserEntry.File("foobar.txt", 42000, Date())
    val sampleFiles = listOf(sample)

    @Before
    fun setUp() {
        fileBrowser = mock()
        useCase = BoxProviderUseCase(object: VolumeManager {
            override val roots: List<VolumeRoot>
                get() = volumes

            override fun fileBrowser(rootID: String) = fileBrowser
        })
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
    fun testQueryChildFromFiles() {
        val lst = useCase.queryChildDocuments(docId.copy(path = BoxPath.Root * "file.txt"))
                .toBlocking().first()
        lst shouldMatch hasSize(equalTo(0))
    }

    @Test
    fun testDownload() {

    }

    @Test
    fun testUpload() {

    }

    @Test
    fun testDelete() {

    }
}
