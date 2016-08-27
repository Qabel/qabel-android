package de.qabel.qabelbox.chat.view.adapters

import android.view.View
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import de.qabel.chat.repository.entities.ChatDropMessage.Direction
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.config.SymmetricKey
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.test.shadows.TextViewFontShadow
import de.qabel.qabelbox.ui.views.TextViewFont
import kotlinx.android.synthetic.main.chat_message_in.view.*
import kotlinx.android.synthetic.main.chat_message_share.view.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDateFormat
import java.net.URI
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class,
        shadows = arrayOf(TextViewFontShadow::class, ShadowDateFormat::class), manifest = "src/main/AndroidManifest.xml")
class ShareChatMessageViewHolderTest {

    @Test
    fun testOutgoing() {
        testViewHolder(Direction.OUTGOING, MessagePayloadDto.ShareMessage.ShareStatus.ACCEPTED, R.string.open)
    }

    @Test
    fun testAcceptedShare() {
        testViewHolder(Direction.INCOMING, MessagePayloadDto.ShareMessage.ShareStatus.ACCEPTED, R.string.open);
    }

    @Test
    fun testNewShare() {
        testViewHolder(Direction.INCOMING, MessagePayloadDto.ShareMessage.ShareStatus.NEW, R.string.accept_share);
    }

    @Test
    fun testUnreachableShare() {
        testViewHolder(Direction.INCOMING, MessagePayloadDto.ShareMessage.ShareStatus.NOT_REACHABLE, R.string.currently_not_available);
    }

    @Test
    fun testDeletedShare() {
        testViewHolder(Direction.INCOMING, MessagePayloadDto.ShareMessage.ShareStatus.DELETED, R.string.permanently_unavailable);
    }

    fun testViewHolder(direction: Direction, status: MessagePayloadDto.ShareMessage.ShareStatus, expectedLabel: Int) {
        val contact = mock<Contact>()
        stub(contact.alias).toReturn("contact")

        val view = mock<View>()
        stub(view.context).toReturn(RuntimeEnvironment.application)
        val messageField: TextViewFont = mock()
        val actionField: TextViewFont = mock();
        val dateField: TextViewFont = mock()

        stub(view.shareText).toReturn(messageField)
        stub(view.tvDate).toReturn(dateField)
        stub(view.tvLink).toReturn(actionField)

        val holder = ShareChatMessageViewHolder(view)
        val msg = ChatMessage(mock<Identity>(), contact,
                direction, Date(),
                MessagePayloadDto.ShareMessage("text", URI("http://foo"), SymmetricKey(listOf()),
                        status))

        holder.bindTo(msg, false)

        verify(messageField).text = "text"
        verify(actionField).text = RuntimeEnvironment.application.getString(expectedLabel);
        verify(dateField).text = any()
    }

}
