package de.qabel.qabelbox.chat.transformers

import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.core.repository.inmemory.InMemoryContactRepository
import de.qabel.core.repository.inmemory.InMemoryIdentityRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ChatMessageTransformerTest {

    val identity = IdentityHelper.createIdentity("identity", null)
    val contact = IdentityHelper.createContact("contact_name")
    val identityRepository = InMemoryIdentityRepository().apply { save(identity) }
    val contactRepository = InMemoryContactRepository().apply {
        save(contact, identity)
    }
    val chatMessageTransformer = ChatMessageTransformer(identityRepository, contactRepository)
    val now = Date()

    @Test fun testIncomingTextMessage() {
        val text = "foobar"
        val message = ChatDropMessage(1, 1,
                ChatDropMessage.Direction.INCOMING, ChatDropMessage.Status.NEW,
                ChatDropMessage.MessageType.BOX_MESSAGE, "{\"msg\": \"$text\"}", now.time)
        val msg = chatMessageTransformer.transform(message)
        assert(msg.contact == contact)
        assert(msg.identity == identity)
        assert((msg.messagePayload as MessagePayloadDto.TextMessage).message == text)
        assert(msg.direction == ChatDropMessage.Direction.INCOMING)
        assert(msg.time.time == now.time)
    }
}
