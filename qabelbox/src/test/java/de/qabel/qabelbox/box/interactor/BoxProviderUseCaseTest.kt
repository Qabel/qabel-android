package de.qabel.qabelbox.box.interactor

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.DocumentId
import org.junit.Before
import org.junit.Test
import rx.lang.kotlin.toSingletonObservable
import java.util.*

class BoxProviderUseCaseTest {

    lateinit var useCase: BoxProviderUseCase
    lateinit var fileBrowser: FileBrowserUseCase
    val volumes = listOf(VolumeRoot("root", "docid", "alias"))
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
        val id = DocumentId("id", "pref", BoxPath.Root)
        whenever(fileBrowser.list(BoxPath.Root)).thenReturn(sampleFiles.toSingletonObservable())
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
