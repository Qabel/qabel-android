package de.qabel.qabelbox.box.interactor

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.core.config.Identities
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.toDocumentId
import de.qabel.qabelbox.repositories.MockIdentityRepository
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import rx.lang.kotlin.toSingletonObservable
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class , constants = BuildConfig::class)
class BoxVolumeManagerTest {

    val identity = IdentityHelper.createIdentity("name", "prefix")
    val repo = MockIdentityRepository(identity)
    val docId = DocumentId(identity.keyIdentifier, identity.prefixes.first(), BoxPath.Root)
    val volume = VolumeRoot(docId.toString().dropLast(1), docId.toString(), identity.alias)
    lateinit var manager: VolumeManager
    lateinit var fileBrowser: FileBrowserUseCase

    @Before
    fun setUp() {
        fileBrowser = mock()
    }

    @Test
    fun testGetRoots() {
        manager = BoxVolumeManager(repo) { fileBrowser }
        manager.roots shouldMatch equalTo(listOf(volume))
    }

    @Test
    fun testFileBrowser() {
        manager = BoxVolumeManager(repo) {
            it shouldMatch equalTo(volume)
            fileBrowser
        }
        manager.fileBrowser(volume.rootID) shouldMatch sameInstance(fileBrowser)
    }
}
