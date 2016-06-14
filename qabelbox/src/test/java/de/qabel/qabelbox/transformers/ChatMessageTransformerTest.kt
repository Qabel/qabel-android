package de.qabel.qabelbox.transformers

import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.ChatMessageItem
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.dto.MessagePayload
import de.qabel.qabelbox.repositories.MockContactRepository
import de.qabel.qabelbox.repositories.MockIdentityRepository
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
    val identityRepository = MockIdentityRepository(identity)
    val contactRepository = MockContactRepository(contact)
    val chatMessageTransformer = ChatMessageTransformer(identityRepository, contactRepository)
    val now = Date()

    @Test fun testIncomingTextMessage() {
        val text = "foobar"
        val message = ChatMessageItem(1, 1, now.time, contact.keyIdentifier, identity.keyIdentifier,
                "1", ChatMessageItem.BOX_MESSAGE, "{\"msg\": \"$text\"}")
        message.time_stamp = now.time
        val msg = chatMessageTransformer.transform(message)
        assert(msg.contact == contact)
        assert(msg.identity == identity)
        assert((msg.messagePayload as MessagePayload.TextMessage).message == text)
        assert(msg.direction == ChatMessage.Direction.INCOMING)
    }
}
