package de.qabel.qabelbox.box.interactor

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.box.storage.*
import de.qabel.box.storage.dto.BoxPath
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.local.LocalStorage
import de.qabel.box.storage.local.MockLocalStorage
import de.qabel.qabelbox.*
import de.qabel.qabelbox.box.BoxScheduler
import de.qabel.qabelbox.box.backends.MockStorageBackend
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.UploadSource
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.util.IdentityHelper
import de.qabel.qabelbox.util.toUploadSource
import de.qabel.qabelbox.util.waitFor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import rx.schedulers.Schedulers
import java.io.InputStream
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class BoxOperationFileBrowserTest() {

    val identity = IdentityHelper.createIdentity("identity", null)
    val storage = MockStorageBackend()
    val localStorage = MockLocalStorage()
    val docId = DocumentId(identity.keyIdentifier, identity.prefixes.first().prefix, BoxPath.Root)

    lateinit var useCase: OperationFileBrowser

    val samplePayload: String = "payload"
    val sampleName = "sampleName"
    val sample = BrowserEntry.File(sampleName, 42, Date())

    @Before
    fun setUp() {
        val prefix = identity.prefixes.first()
        val keys = BoxReadFileBrowser.KeyAndPrefix(identity)
        val volume = AndroidBoxVolume(BoxVolumeConfig(
                prefix.prefix,
                RootRefCalculator().rootFor(identity.primaryKeyPair.privateKey, prefix.type, prefix.prefix),
                byteArrayOf(1),
                storage,
                storage,
                "Blake2b",
                createTempDir()), identity.primaryKeyPair)
        val navigator = BoxVolumeNavigator(keys, volume)
        useCase = BoxOperationFileBrowser(keys, navigator, mock(), localStorage, BoxScheduler(Schedulers.immediate()))
    }

    @Test
    fun roundTripFile() {
        val path = BoxPath.Root * sampleName
        val file = createTempFile()

        useCase.upload(path, samplePayload.toUploadSource(sample)).second.waitFor()
        useCase.download(path, file.outputStream()).second.waitFor().apply {
            file.readText() isEqual samplePayload
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
        val file = createTempFile()
        useCase.upload(path, samplePayload.toUploadSource(sample)).second.waitFor()
        useCase.query(path.parent) evalsTo BrowserEntry.Folder(path.parent.name)
        useCase.download(path, file.outputStream()).second.waitFor().apply {
            file.readText() isEqual samplePayload
        }
    }

    @Test
    fun deleteFile() {
        val path = BoxPath.Root / "firstFolder" / "subFolder" * sampleName
        useCase.upload(path, samplePayload.toUploadSource(sample)).second.waitFor()
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
    fun failedUpload() {
        val nav: IndexNavigation = mockedIndexNavigation()
        val e = QblStorageException("test")
        whenever(nav.upload(any<String>(), any<InputStream>(), any(), any())).thenThrow(e)

        useCase.upload(BoxPath.Root * "test", UploadSource(mock(), sample)).second errorsWith e
    }

    @Test
    fun failedDownload() {
        val nav: IndexNavigation = mockedIndexNavigation()
        val e = QblStorageException("test")
        whenever(nav.getFile(any())).thenThrow(e)

        useCase.download(BoxPath.Root * "test", createTempFile().outputStream()).second errorsWith e
    }

    @Test
    fun failedDelete() {
        val nav: IndexNavigation = mockedIndexNavigation()
        val e = QblStorageException("test")
        whenever(nav.getFolder(any())).thenThrow(e)

        useCase.delete(BoxPath.Root / "test") errorsWith e
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
        val keys = BoxReadFileBrowser.KeyAndPrefix("key", "prefix")
        useCase = BoxOperationFileBrowser(keys, BoxVolumeNavigator(keys, volume), mock(), localStorage, BoxScheduler(Schedulers.immediate()))
        return nav
    }
}
