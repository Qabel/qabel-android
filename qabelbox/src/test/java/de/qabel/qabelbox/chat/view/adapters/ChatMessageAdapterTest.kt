package de.qabel.qabelbox.chat.view.adapters

import android.widget.LinearLayout
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.chat.dto.SymmetricKey
import de.qabel.qabelbox.checkNull
import de.qabel.qabelbox.helper.FontHelper
import de.qabel.qabelbox.test.shadows.TextViewFontShadow
import de.qabel.qabelbox.ui.views.TextViewFont
import org.junit.Before
import org.junit.Ignore
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
        message = ChatMessage(mock(), mock(), ChatDropMessage.Direction.INCOMING,
                Date(), MessagePayloadDto.TextMessage("Text"))
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
        val second = message.copy(direction = ChatDropMessage.Direction.OUTGOING)
        adapter.messages = listOf(message, second)
        adapter.getItemAtPosition(0)!! shouldMatch equalTo(message)
        adapter.getItemAtPosition(1)!! shouldMatch equalTo(second)
        adapter.getItemAtPosition(2) shouldMatch equalTo(null as ChatMessage?)
    }

    @Test
    fun differentViewsForShareAndTextMessage() {
        val shareMessage = message.copy(messagePayload =
        MessagePayloadDto.ShareMessage("share message", URL("http://foo"), SymmetricKey(listOf())))
        adapter.messages = listOf(message, shareMessage,
                message.copy(direction = ChatDropMessage.Direction.OUTGOING),
                shareMessage.copy(direction = ChatDropMessage.Direction.OUTGOING))
        assertThat(adapter.getItemViewType(0), equalTo(ChatMessageAdapter.MessageType.TEXT_IN.ordinal))
        assertThat(adapter.getItemViewType(1), equalTo(ChatMessageAdapter.MessageType.SHARE_IN.ordinal))
        assertThat(adapter.getItemViewType(2), equalTo(ChatMessageAdapter.MessageType.TEXT_OUT.ordinal))
        assertThat(adapter.getItemViewType(3), equalTo(ChatMessageAdapter.MessageType.SHARE_OUT.ordinal))
    }

    @Test
    fun createsTextViewHolder() {
        for (type in listOf(ChatMessageAdapter.MessageType.TEXT_IN.ordinal, ChatMessageAdapter.MessageType.TEXT_OUT.ordinal)) {
            val holder = adapter.onCreateViewHolder(
                    LinearLayout(RuntimeEnvironment.application), type)
            check(holder is TextChatMessageViewHolder)
            checkNotNull(holder.itemView?.findViewById(R.id.tvText) as? TextViewFont)
            checkNotNull(holder.itemView?.findViewById(R.id.tvDate) as? TextViewFont)
            checkNull(holder.itemView?.findViewById(R.id.messageFileContainer)?.visibility)
        }
    }

    @Test
    fun createsShareViewHolder() {
        for (type in listOf(ChatMessageAdapter.MessageType.SHARE_IN.ordinal, ChatMessageAdapter.MessageType.SHARE_OUT.ordinal)) {
            val holder = adapter.onCreateViewHolder(
                    LinearLayout(RuntimeEnvironment.application), type)
            check(holder is ShareChatMessageViewHolder)
            checkNotNull(holder.itemView?.findViewById(R.id.shareText) as? TextViewFont)
            checkNotNull(holder.itemView?.findViewById(R.id.tvDate) as? TextViewFont)
            checkNotNull(holder.itemView?.findViewById(R.id.messageFileContainer))
        }
    }

    @Test
    @Ignore("TODO method is calling baseClass")
    fun bindViewHolder() {
        adapter.messages = listOf(message)
        val holder = mock<TextChatMessageViewHolder>()
        adapter.onBindViewHolder(holder, 0)
        verify(holder).bindTo(message, false)
    }
}
