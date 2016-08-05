package de.qabel.qabelbox.chat.interactor

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.mock
import de.qabel.core.crypto.BinaryDropMessageV0
import de.qabel.core.drop.DropMessage
import de.qabel.core.repository.ChatDropMessageRepository
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.qabelbox.eq
import de.qabel.qabelbox.tmp_core.InMemoryChatDropMessageRepository
import de.qabel.qabelbox.tmp_core.InMemoryContactRepository
import de.qabel.qabelbox.tmp_core.InMemoryIdentityRepository
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import org.spongycastle.util.encoders.Base64


class Base64DropReceiverTest {
    @Test
    fun receive() {
        val repository: ChatDropMessageRepository = InMemoryChatDropMessageRepository()
        val identityRepository = InMemoryIdentityRepository()
        val receiverEntity = IdentityHelper.createIdentity("receiver", null)
        identityRepository.save(receiverEntity)
        val contactRepository = InMemoryContactRepository()
        val senderEntity = IdentityHelper.createIdentity("sender", null)
        contactRepository.save(senderEntity.toContact(), receiverEntity)

        val message: DropMessage = DropMessage(senderEntity, """{ "msg": "foobar"}""",
                ChatDropMessage.MessageType.BOX_MESSAGE.name)
        val binaryMessage = BinaryDropMessageV0(message)
                .assembleMessageFor(receiverEntity.toContact(), senderEntity)

        val receiver = Base64DropReceiver(
                repository = repository,
                identityRepository = identityRepository,
                contactRepository = contactRepository,
                notificationManager = mock())
        val msg = Base64.encode(binaryMessage).map { it.toChar() }.joinToString("")
        receiver.receive("foo", msg)
        val new = repository.findNew(receiverEntity.id)
        new shouldMatch hasSize(equalTo(1))
        new.first().let {
            it.direction eq ChatDropMessage.Direction.INCOMING
            it.identityId eq receiverEntity.id
        }
    }

}
