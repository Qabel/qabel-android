package de.qabel.qabelbox.box.interactor

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.box.storage.BoxVolume
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.box.backends.MockStorageBackend
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.util.IdentityHelper
import de.qabel.qabelbox.util.asString
import de.qabel.qabelbox.util.toUploadSource
import de.qabel.qabelbox.util.waitFor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class BoxFileBrowserUseCaseTest {

    val identityA = IdentityHelper.createIdentity("identity", null)
    val storage = MockStorageBackend()
    val deviceId = byteArrayOf(1,2,3)
    val volume = BoxVolume(storage, storage, identityA.primaryKeyPair,
            deviceId, createTempDir(), "prefix")
    val useCase = MockFileBrowserUseCase()

    val samplePayload = "payload"
    val sampleName = "sampleName"
    val sample = BrowserEntry.File("foo.txt", 42, Date())

    @Test
    fun roundTripFile() {
        val path = BoxPath.Root * sampleName
        useCase.upload(path, samplePayload.toUploadSource(sample)).waitFor()
        useCase.download(path).waitFor().apply {
            asString() shouldMatch equalTo(samplePayload)
        }
    }

}

