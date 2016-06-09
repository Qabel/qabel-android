package de.qabel.qabelbox.ui.adapters

import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.adapter.ChatMessageAdapter
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.dto.MessagePayload
import de.qabel.qabelbox.dto.SymmetricKey
import de.qabel.qabelbox.helper.FontHelper
import de.qabel.qabelbox.test.shadows.TextViewFontShadow
import de.qabel.qabelbox.views.TextViewFont
import kotlinx.android.synthetic.main.item_chat_message_in.view.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.URL
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class,
        shadows = arrayOf(TextViewFontShadow::class), manifest = "src/main/AndroidManifest.xml")
class ChatMessageAdapterTest {

    lateinit var adapter: ChatMessageAdapter
    lateinit var message: ChatMessage

    @Before
    fun setUp() {
        message = ChatMessage(mock(), mock(), ChatMessage.Direction.INCOMING,
            Date(), MessagePayload.TextMessage("Text"))
        adapter = ChatMessageAdapter(listOf())
        FontHelper.disable = true;
    }

    @Test
    fun testGetItemCount() {
        adapter.itemCount shouldMatch  equalTo(0)
        adapter.messages = listOf(message, message)
        adapter.itemCount shouldMatch equalTo(2)
    }

    @Test
    fun itemAt() {
        val second = message.copy(direction = ChatMessage.Direction.OUTGOING)
        adapter.messages = listOf(message, second)
        adapter.getItemAtPosition(0)!! shouldMatch equalTo(message)
        adapter.getItemAtPosition(1)!! shouldMatch equalTo(second)
        adapter.getItemAtPosition(2) shouldMatch equalTo(null as ChatMessage?)
    }

    @Test
    fun differentViewsForShareAndTextMessage() {
        val shareMessage = message.copy(messagePayload =
            MessagePayload.ShareMessage("share message", URL("http://foo"), SymmetricKey(listOf())))
        val noMessage = message.copy(messagePayload = MessagePayload.NoMessage)
        adapter.messages = listOf(message, shareMessage, noMessage,
                message.copy(direction = ChatMessage.Direction.OUTGOING),
                shareMessage.copy(direction = ChatMessage.Direction.OUTGOING),
                noMessage.copy(direction = ChatMessage.Direction.OUTGOING))
        assertThat(adapter.getItemViewType(0), equalTo(ChatMessageAdapter.INCOMING_TEXT))
        assertThat(adapter.getItemViewType(1), equalTo(ChatMessageAdapter.INCOMING_SHARE))
        assertThat(adapter.getItemViewType(2), equalTo(ChatMessageAdapter.NO_MESSAGE))
        assertThat(adapter.getItemViewType(3), equalTo(ChatMessageAdapter.OUTGOING_TEXT))
        assertThat(adapter.getItemViewType(4), equalTo(ChatMessageAdapter.OUTGOING_SHARE))
        assertThat(adapter.getItemViewType(5), equalTo(ChatMessageAdapter.NO_MESSAGE))
    }

    @Test
    fun createsTextViewHolder() {
        for (type in listOf(ChatMessageAdapter.INCOMING_TEXT, ChatMessageAdapter.OUTGOING_TEXT)) {
            val holder = adapter.onCreateViewHolder(
                    LinearLayout(RuntimeEnvironment.application), type)
            checkNotNull(holder?.itemView?.findViewById(R.id.tvText) as? TextViewFont)
            checkNotNull(holder?.itemView?.findViewById(R.id.tvDate) as? TextViewFont)
            check(holder?.itemView?.findViewById(R.id.messageFileContainer) == null)
        }
    }

    @Test
    fun createsShareViewHolder() {
        for (type in listOf(ChatMessageAdapter.INCOMING_SHARE, ChatMessageAdapter.OUTGOING_SHARE)) {
            val holder = adapter.onCreateViewHolder(
                    LinearLayout(RuntimeEnvironment.application), type)
            checkNotNull(holder?.itemView?.findViewById(R.id.tvText) as? TextViewFont)
            checkNotNull(holder?.itemView?.findViewById(R.id.tvDate) as? TextViewFont)
            checkNotNull(holder?.itemView?.findViewById(R.id.messageFileContainer) as? RelativeLayout)
        }
    }

    @Test
    fun createsEmptyMessageViewHolder() {
        val holder = adapter.onCreateViewHolder(
                LinearLayout(RuntimeEnvironment.application), ChatMessageAdapter.NO_MESSAGE)
        check(holder?.itemView?.findViewById(R.id.messageFileContainer) == null)
        check(holder?.itemView?.findViewById(R.id.tvText) == null)
    }

    @Test
    fun viewHolderCanBindItself() {
        val contact = mock<Contact>()
        stub(contact.alias).toReturn("contact")
        val view = mock<View>()
        val text: TextViewFont = mock()
        val date: TextViewFont = mock()
        stub(view.tvText).toReturn(text)
        stub(view.tvDate).toReturn(date)
        val holder = ChatMessageViewHolder(view)
        val msg = ChatMessage(mock<Identity>(), contact,
                ChatMessage.Direction.INCOMING, Date(), MessagePayload.TextMessage("text"))
        holder.bindTo(msg)
        verify(text).text = "text"
        verify(date).text  = any()
    }

    @Test
    fun bindViewHolder() {
        adapter.messages = listOf(message)
        val holder: ChatMessageViewHolder = mock()
        adapter.onBindViewHolder(holder, 0)
        verify(holder).bindTo(message)
    }
}
