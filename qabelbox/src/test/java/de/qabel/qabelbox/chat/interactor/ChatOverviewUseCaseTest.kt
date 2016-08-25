package de.qabel.qabelbox.chat.interactor

import de.qabel.core.extensions.CoreTestCase
import de.qabel.core.extensions.createContact
import de.qabel.core.extensions.createIdentity
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.chat.repository.inmemory.InMemoryChatDropMessageRepository
import de.qabel.core.repository.inmemory.InMemoryContactRepository
import de.qabel.core.repository.inmemory.InMemoryIdentityRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ChatOverviewUseCaseTest : CoreTestCase {

    val identity = createIdentity("Alice")
    val contact = createContact("Bob")

    val contactRepo = InMemoryContactRepository().apply {
        save(contact, identity)
    }
    val identityRepo = InMemoryIdentityRepository().apply {
        save(identity)
    }
    val chatRepo = InMemoryChatDropMessageRepository()
    val useCase = MainChatOverviewUseCase(chatRepo, ChatMessageTransformer(identityRepo, contactRepo))

    @Before
    fun setUp() {
        chatRepo.persist(createMsg("blubb"))
        chatRepo.persist(createMsg("blubb blubb"))
        chatRepo.persist(createMsg("1", ChatDropMessage.Status.NEW))
        chatRepo.persist(createMsg("2", ChatDropMessage.Status.NEW))
        chatRepo.persist(createMsg("3", ChatDropMessage.Status.NEW))
        chatRepo.persist(createMsg("4", ChatDropMessage.Status.NEW))
    }

    @Test
    fun testLoadLatest() {
        val result = useCase.findLatest(identity).toList().toBlocking().first()
        assertThat(result, hasSize(1))
        val conversationDto = result.first()
        assertThat(conversationDto.newMsgCount, equalTo(4))
        assertThat(conversationDto.message.messagePayload.toMessage(), equalTo("4"))
    }

    private fun createMsg(text: String, status: ChatDropMessage.Status = ChatDropMessage.Status.READ) =
            ChatDropMessage(contact.id, identity.id,
                    ChatDropMessage.Direction.OUTGOING, status,
                    ChatDropMessage.MessageType.BOX_MESSAGE, ChatDropMessage.MessagePayload.TextMessage(text), Date().time)

}
