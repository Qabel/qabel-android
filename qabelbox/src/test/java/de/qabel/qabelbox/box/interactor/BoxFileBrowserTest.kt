package de.qabel.qabelbox.box.interactor

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.box.storage.BoxVolume
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.box.backends.MockStorageBackend
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.UploadSource
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.util.IdentityHelper
import de.qabel.qabelbox.util.asString
import de.qabel.qabelbox.util.toUploadSource
import de.qabel.qabelbox.util.waitFor
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import rx.Observable
import java.util.Date

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class BoxFileBrowserTest {

    val identity = IdentityHelper.createIdentity("identity", null)
    val storage = MockStorageBackend()
    val deviceId = byteArrayOf(1,2,3)
    val docId = DocumentId(identity.keyIdentifier, identity.prefixes.first(), BoxPath.Root)
    val volume = VolumeRoot(docId.toString().dropLast(1), docId.toString(), identity.alias)
    //val volume = BoxVolume(storage, storage, identityA.primaryKeyPair,
    //        deviceId, createTempDir(), "prefix")
    lateinit var useCase: FileBrowser

    val samplePayload = "payload"
    val sampleName = "sampleName"
    val sample = BrowserEntry.File(sampleName, 42, Date())

    @Before
    fun setUp() {
         useCase = BoxFileBrowser(identity, storage, storage, byteArrayOf(1), createTempDir())
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
        storage.storage.size eq 3 // index and 2 folder metadata files and no file
        useCase.query(path.parent) evalsTo BrowserEntry.Folder(path.parent.name)
    }

    @Test
    fun deleteFolder() {
        val path = BoxPath.Root / "firstFolder" / "subFolder"
        useCase.createFolder(path).waitFor()
        useCase.delete(path).waitFor()
        storage.storage.size eq 2 // index and 1 folder metadata file
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

}

infix fun <T> T.eq(thing: T) {
    assertThat(this, equalTo(thing))
}

infix fun <T> Observable<T>.evalsTo(thing: T) {
    assertThat(this.toBlocking().first(), equalTo(thing))
}
