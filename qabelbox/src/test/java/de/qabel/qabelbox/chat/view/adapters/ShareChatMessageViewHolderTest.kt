package de.qabel.qabelbox.chat.view.adapters

import android.view.View
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayload
import de.qabel.qabelbox.chat.dto.SymmetricKey
import de.qabel.qabelbox.chat.view.adapters.ShareChatMessageViewHolder
import de.qabel.qabelbox.test.shadows.TextViewFontShadow
import de.qabel.qabelbox.ui.views.TextViewFont
import kotlinx.android.synthetic.main.chat_message_in.view.*
import kotlinx.android.synthetic.main.chat_message_share.view.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDateFormat
import java.net.URL
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class,
        shadows = arrayOf(TextViewFontShadow::class, ShadowDateFormat::class), manifest = "src/main/AndroidManifest.xml")
class ShareChatMessageViewHolderTest {

    @Test
    fun testOutgoing() {
        testViewHolder(ChatMessage.Direction.OUTGOING, MessagePayload.ShareMessage.ShareStatus.ACCEPTED, R.string.open)
    }

    @Test
    fun testAcceptedShare() {
        testViewHolder(ChatMessage.Direction.INCOMING, MessagePayload.ShareMessage.ShareStatus.ACCEPTED, R.string.open);
    }

    @Test
    fun testNewShare() {
        testViewHolder(ChatMessage.Direction.INCOMING, MessagePayload.ShareMessage.ShareStatus.NEW, R.string.accept_share);
    }

    @Test
    fun testUnreachableShare() {
        testViewHolder(ChatMessage.Direction.INCOMING, MessagePayload.ShareMessage.ShareStatus.NOT_REACHABLE, R.string.currently_not_available);
    }

    @Test
    fun testDeletedShare() {
        testViewHolder(ChatMessage.Direction.INCOMING, MessagePayload.ShareMessage.ShareStatus.DELETED, R.string.permanently_unavailable);
    }

    fun testViewHolder(direction: ChatMessage.Direction, status: MessagePayload.ShareMessage.ShareStatus, expectedLabel: Int) {
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
                MessagePayload.ShareMessage("text", URL("http://foo"), SymmetricKey(listOf()),
                        status))

        holder.bindTo(msg, false)

        verify(messageField).text = "text"
        verify(actionField).text = RuntimeEnvironment.application.getString(expectedLabel);
        verify(dateField).text = any()
    }

}
