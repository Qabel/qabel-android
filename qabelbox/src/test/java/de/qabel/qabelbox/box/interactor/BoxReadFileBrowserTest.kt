package de.qabel.qabelbox.box.interactor

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.box.storage.*
import de.qabel.box.storage.dto.BoxPath
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.local.MockLocalStorage
import de.qabel.core.config.Prefix
import de.qabel.qabelbox.*
import de.qabel.qabelbox.box.BoxScheduler
import de.qabel.qabelbox.box.backends.MockStorageBackend
import de.qabel.qabelbox.box.dto.BrowserEntry
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
import java.io.File
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class BoxReadFileBrowserTest {

    val identity = IdentityHelper.createIdentity("identity", null)
    val storage = MockStorageBackend()
    val localStorage = MockLocalStorage()
    val docId = DocumentId(identity.keyIdentifier, identity.prefixes.first().prefix, BoxPath.Root)

    lateinit var prepareUseCase: OperationFileBrowser
    lateinit var useCase: ReadFileBrowser

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
        val navigator = BoxVolumeNavigator(keys, volume, localStorage)
        val boxScheduler = BoxScheduler(Schedulers.immediate())
        useCase = BoxReadFileBrowser(keys, navigator, mock(), boxScheduler)
        prepareUseCase = BoxOperationFileBrowser(keys, navigator, mock(), localStorage, boxScheduler)
    }

    @Test
    fun asDocumentId() {
        useCase.asDocumentId(BoxPath.Root) evalsTo docId
    }


    @Test
    fun list() {
        val folder = BoxPath.Root / "firstFolder"
        val subfolderFile = folder * sampleName
        val file = BoxPath.Root * sampleName
        prepareUseCase.upload(file, samplePayload.toUploadSource(sample)).second.waitFor()
        prepareUseCase.upload(subfolderFile, samplePayload.toUploadSource(sample)).second.waitFor()
        prepareUseCase.createFolder(folder).waitFor()

        val listing = useCase.list(BoxPath.Root).toBlocking().first().map { it.name }.toSet()
        val subfolderListing = useCase.list(folder).toBlocking().first().map { it.name }.toSet()

        listing eq setOf(sample.name, "firstFolder")
        subfolderListing eq setOf(sample.name)
    }

    @Test
    fun listIsSorted() {
        val root = BoxPath.Root
        val file = root * "aaa"
        val file2 = root * "zzz"
        val folder1 = root / "AAA"
        val folder2 = root / "BBB"
        prepareUseCase.upload(file2, samplePayload.toUploadSource(sample)).second.waitFor()
        prepareUseCase.upload(file, samplePayload.toUploadSource(sample)).second.waitFor()
        prepareUseCase.createFolder(folder2).waitFor()
        prepareUseCase.createFolder(folder1).waitFor()

        val listing = useCase.list(BoxPath.Root).toBlocking().first().map { it.name }

        listing eq listOf("AAA", "BBB", "aaa", "zzz")
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
    fun failedList() {
        val nav: IndexNavigation = mockedIndexNavigation()
        val e = QblStorageException("test")
        whenever(nav.refresh()).thenThrow(e)

        useCase.list(BoxPath.Root) errorsWith e
    }


    private fun mockedIndexNavigation(): IndexNavigation {
        val volume: BoxVolume = mock()
        val keys = BoxReadFileBrowser.KeyAndPrefix("key", "prefix")
        whenever(volume.config).thenReturn(BoxVolumeConfig(
                keys.prefix,
                RootRefCalculator().rootFor(identity.primaryKeyPair.privateKey, Prefix.TYPE.USER, keys.prefix),
                byteArrayOf(1),
                storage,
                storage,
                "Blake2b",
                mock()))

        val nav: IndexNavigation = mock()
        stubMethod(volume.navigate(), nav)
        stubMethod(nav.metadata, mock())
        stubMethod(nav.metadata.path, File.createTempFile("bla",""))
        useCase = BoxReadFileBrowser(keys, BoxVolumeNavigator(keys, volume, localStorage), mock(), BoxScheduler(Schedulers.immediate()))
        return nav
    }


}
