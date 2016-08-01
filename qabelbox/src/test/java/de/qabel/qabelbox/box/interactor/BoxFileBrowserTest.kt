package de.qabel.qabelbox.box.interactor

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.box.storage.AndroidBoxVolume
import de.qabel.box.storage.BoxVolume
import de.qabel.box.storage.BoxVolumeConfig
import de.qabel.box.storage.IndexNavigation
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.qabelbox.*
import de.qabel.qabelbox.box.backends.MockStorageBackend
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.UploadSource
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.util.IdentityHelper
import de.qabel.qabelbox.util.asString
import de.qabel.qabelbox.util.toUploadSource
import de.qabel.qabelbox.util.waitFor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.io.InputStream
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class BoxFileBrowserTest {

    val identity = IdentityHelper.createIdentity("identity", null)
    val storage = MockStorageBackend()
    val docId = DocumentId(identity.keyIdentifier, identity.prefixes.first(), BoxPath.Root)
    lateinit var useCase: FileBrowser

    val samplePayload = "payload"
    val sampleName = "sampleName"
    val sample = BrowserEntry.File(sampleName, 42, Date())

    @Before
    fun setUp() {
        val prefix = identity.prefixes.first()
        val volume = AndroidBoxVolume(BoxVolumeConfig(
                prefix,
                byteArrayOf(1),
                storage,
                storage,
                "Blake2b",
                createTempDir()), identity.primaryKeyPair)
        val keyAndPrefix = BoxFileBrowser.KeyAndPrefix(identity.keyIdentifier, identity.prefixes.first())
        useCase = BoxFileBrowser(
                keyAndPrefix, BoxVolumeInteractor(volume, keyAndPrefix))
    }

    @Test
    fun asDocumentId() {
        useCase.asDocumentId(BoxPath.Root) evalsTo docId
    }

    @Test
    fun roundTripFile() {
        val path = BoxPath.Root * sampleName
        useCase.upload(path, samplePayload.toUploadSource(sample)).waitFor()
        useCase.download(path).waitFor().apply {
            asString() shouldMatch equalTo(samplePayload)
        }
    }

    @Test
    fun createSubfolder() {
        val path = BoxPath.Root / "firstFolder" / "subFolder"
        useCase.createFolder(path).waitFor()
        useCase.query(path.parent) evalsTo BrowserEntry.Folder(path.parent.name)
        useCase.query(path) evalsTo BrowserEntry.Folder(path.name)
    }

    @Test
    fun uploadInSubfolder() {
        val path = BoxPath.Root / "firstFolder" / "subFolder" * sampleName
        useCase.upload(path, samplePayload.toUploadSource(sample)).waitFor()
        useCase.query(path.parent) evalsTo BrowserEntry.Folder(path.parent.name)
        useCase.download(path).waitFor().apply {
            asString() shouldMatch equalTo(samplePayload)
        }
    }

    @Test
    fun deleteFile() {
        val path = BoxPath.Root / "firstFolder" / "subFolder" * sampleName
        useCase.upload(path, samplePayload.toUploadSource(sample)).waitFor()
        useCase.delete(path).waitFor()
        // folder exists
        useCase.query(path.parent) evalsTo BrowserEntry.Folder(path.parent.name)
        // folder is empty
        useCase.list(path.parent) evalsTo emptyList()
    }

    @Test
    fun deleteFolder() {
        val path = BoxPath.Root / "firstFolder" / "subFolder"
        useCase.createFolder(path).waitFor()
        useCase.delete(path).waitFor()

        useCase.list(path.parent) evalsTo emptyList()
        useCase.query(path.parent) evalsTo BrowserEntry.Folder(path.parent.name)

    }

    @Test
    fun list() {
        val folder = BoxPath.Root / "firstFolder"
        val subfolderFile = folder * sampleName
        val file = BoxPath.Root * sampleName
        useCase.upload(file, samplePayload.toUploadSource(sample)).waitFor()
        useCase.upload(subfolderFile , samplePayload.toUploadSource(sample)).waitFor()
        useCase.createFolder(folder).waitFor()

        val listing = useCase.list(BoxPath.Root).toBlocking().first().map { it.name }.toSet()
        val subfolderListing = useCase.list(folder).toBlocking().first().map { it.name }.toSet()

        listing eq setOf(sample.name, "firstFolder")
        subfolderListing eq setOf(sample.name)
    }

    @Test
    fun queryRoot() {
        val entry = useCase.query(BoxPath.Root).toBlocking().first()
        entry.name shouldMatch equalTo("")
    }



    @Test
    fun failedQuery() {
        val nav: IndexNavigation = mockedIndexNavigation()

        val e = QblStorageException("test")
        whenever(nav.listFiles()).thenThrow(e)

        useCase.query(BoxPath.Root * "test") errorsWith e
    }

    @Test
    fun failedUpload() {
        val nav: IndexNavigation = mockedIndexNavigation()
        val e = QblStorageException("test")
        whenever(nav.upload(any<String>(), any<InputStream>(), any())).thenThrow(e)

        useCase.upload(BoxPath.Root * "test", UploadSource(mock(), sample)) errorsWith e
    }

    @Test
    fun failedDownload() {
        val nav: IndexNavigation = mockedIndexNavigation()
        val e = QblStorageException("test")
        whenever(nav.listFiles()).thenThrow(e)

        useCase.download(BoxPath.Root * "test") errorsWith e
    }

    @Test
    fun failedDelete() {
        val nav: IndexNavigation = mockedIndexNavigation()
        val e = QblStorageException("test")
        whenever(nav.getFolder(any())).thenThrow(e)

        useCase.delete(BoxPath.Root / "test") errorsWith e
    }

    @Test
    fun failedList() {
        val nav: IndexNavigation = mockedIndexNavigation()
        val e = QblStorageException("test")
        whenever(nav.reloadMetadata()).thenThrow(e)

        useCase.list(BoxPath.Root) errorsWith e
    }

    @Test
    fun failedCreateFolder() {
        val nav: IndexNavigation = mockedIndexNavigation()
        val e = QblStorageException("test")
        whenever(nav.createFolder(any<String>())).thenThrow(e)

        useCase.createFolder(BoxPath.Root / "Folder") errorsWith e
    }

    private fun mockedIndexNavigation(): IndexNavigation {
        val volume: BoxVolume = mock()
        val nav: IndexNavigation = mock()
        stubMethod(volume.navigate(), nav)
        val keyAndPrefix = BoxFileBrowser.KeyAndPrefix("key", "prefix")
        useCase = BoxFileBrowser(keyAndPrefix, BoxVolumeInteractor(volume, keyAndPrefix))
        return nav
    }


}
