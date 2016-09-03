package de.qabel.qabelbox.box.interactor

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.*
import de.qabel.box.storage.AndroidBoxVolume
import de.qabel.box.storage.BoxVolumeConfig
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.chat.service.ChatService
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.box.backends.MockStorageBackend
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.eq
import de.qabel.qabelbox.util.IdentityHelper
import de.qabel.qabelbox.util.toUploadSource
import de.qabel.qabelbox.util.waitFor
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.util.*

@Ignore("needs factoring, core changed, receiving incredible nullpointers...")
@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class BoxSharerTest {

    val identity = IdentityHelper.createIdentity("identity", null)
    val storage = MockStorageBackend()
    lateinit var useCase: BoxFileBrowser
    lateinit var sharer: BoxSharer

    val contact = IdentityHelper.createContact("contact_name")

    val samplePayload = "payload"
    val sampleName = "sampleName"
    val sample = BrowserEntry.File(sampleName, 42, Date())
    val path = BoxPath.Root * sampleName
    lateinit var chatService: ChatService

    init {
        // a sample message to allow for mocking
        MockitoKotlin.registerInstanceCreator {
            ChatDropMessage(1, 1, ChatDropMessage.Direction.INCOMING,
                    ChatDropMessage.Status.PENDING,
                    ChatDropMessage.MessageType.BOX_MESSAGE,
                    """{"msg": "foo"}""", 1)
        }
    }

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
        useCase = BoxFileBrowser(
                BoxFileBrowser.KeyAndPrefix(identity.keyIdentifier, prefix),
                volume)
        chatService = mock()
        sharer = BoxSharer(useCase, chatService, identity, storage)
    }

    @Test
    fun testSendFileShare() {
        share()
        shareMessageWasSent()
        val (obj, nav) = useCase.queryObjectAndNav(path)
        nav.getSharesOf(obj) shouldMatch hasSize(equalTo(1))
    }

    private fun shareMessageWasSent() {
        verify(chatService).sendMessage(capture {
            it.contactId eq contact.id
            it.identityId eq identity.id
            it.direction eq ChatDropMessage.Direction.OUTGOING
            it.messageType eq ChatDropMessage.MessageType.SHARE_NOTIFICATION
        })
    }

    @Test
    fun testDuplicateShare() {
        share()
        reset(chatService)
        share()
        shareMessageWasSent()
        val (obj, nav) = useCase.queryObjectAndNav(path)
        nav.getSharesOf(obj) shouldMatch hasSize(equalTo(1))
    }

    private fun share() {
        useCase.upload(path, samplePayload.toUploadSource(sample)).waitFor()
        sharer.sendFileShare(contact, path)
    }
}
