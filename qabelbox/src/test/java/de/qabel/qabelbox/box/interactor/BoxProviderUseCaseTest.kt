package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.DocumentId
import org.junit.Assert.*
import org.mockito.Matchers.*
import org.mockito.Mockito.*
import org.hamcrest.Matchers.*

import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

class BoxProviderUseCaseTest {

    lateinit var useCase: BoxProviderUseCase
    val volumes = listOf(VolumeRoot("root", "docid", "alias"))

    @Before
    fun setUp() {
        useCase = BoxProviderUseCase(object: VolumeManager {
            override val roots: List<VolumeRoot>
                get() = volumes

            override fun fileBrowser(rootID: String) = MockFileBrowserUseCase()
        })
    }

    @Test
    fun testAvailableRoots() {
        assertEquals(useCase.availableRoots(), volumes)
    }
    @Test
    fun testQueryChildDocuments() {
        val id = DocumentId("id", "pref", BoxPath.Root)
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
