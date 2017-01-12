package de.qabel.qabelbox.chat.view.adapters

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.nhaarman.mockito_kotlin.*
import de.qabel.chat.repository.entities.BoxFileChatShare
import de.qabel.chat.repository.entities.ChatDropMessage.Direction
import de.qabel.chat.repository.entities.ShareStatus
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDateFormat
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class,
        qualifiers = "en", shadows = arrayOf(TextViewFontShadow::class, ShadowDateFormat::class), manifest = "src/main/AndroidManifest.xml")
class ShareChatMessageViewHolderTest {

    @Before
    fun setUp(){
        Locale.setDefault(Locale.ENGLISH)
    }

    @Test
    fun testOutgoing() {
        testViewHolder(Direction.OUTGOING, ShareStatus.ACCEPTED, null)
    }

    @Test
    fun testInComingShare() {
        testViewHolder(Direction.INCOMING, ShareStatus.ACCEPTED, null)
    }

    @Test
    fun testUnreachableShare() {
        testViewHolder(Direction.INCOMING, ShareStatus.UNREACHABLE, R.string.currently_not_available)
    }

    @Test
    fun testDeletedShare() {
        testViewHolder(Direction.INCOMING, ShareStatus.DELETED, R.string.permanently_unavailable)
    }

    fun testViewHolder(direction: Direction, status: ShareStatus, expectedLabel: Int?) {
        val contact = mock<Contact>()
        stub(contact.alias).toReturn("contact")
        stub(contact.keyIdentifier).toReturn("key")

        val view = mock<View>()

        stub(view.context).toReturn(RuntimeEnvironment.application)
        val messageField: TextViewFont = mock()
        val fileField: TextViewFont = mock()
        val dateField: TextViewFont = mock()
        val overlay: TextViewFont = mock()
        val image: ImageView = mock()
        val preview: ImageView = mock()
        val messageContainer: LinearLayout = mock()
        val beginIndicator: LinearLayout = mock()
        val beginIndicatorImage: GradientDrawable = mock()
        stub(beginIndicator.background).toReturn(beginIndicatorImage)
        val background = mock<Drawable>().apply {
            stub(this.mutate()).toReturn(mock<GradientDrawable>())
        }
        stub(messageContainer.background).toReturn(background)

        stub(view.message).toReturn(messageField)
        stub(view.tvDate).toReturn(dateField)
        stub(view.file_name).toReturn(fileField)
        stub(view.msg_overlay).toReturn(overlay)
        stub(view.messageFileIcon).toReturn(image)
        stub(view.messageFilePreview).toReturn(preview)
        stub(view.msg_container).toReturn(messageContainer)
        stub(view.contact_avatar).toReturn(mock())
        stub(view.chat_begin_indicator).toReturn(beginIndicator)

        val fileSize = 2048L
        val holder = ShareChatMessageViewHolder(view, {})
        val msg = ChatMessage(mock<Identity>(), contact,
                direction, Date(),
                MessagePayloadDto.ShareMessage("text", BoxFileChatShare(status, "test.txt", fileSize,
                        SymmetricKey(listOf()), "http://foo")))
        holder.bindTo(msg, false)

        verify(messageField).text = "text"
        verify(fileField).text = "test.txt 2.0KB"
        expectedLabel?.
                let { verify(overlay).text = RuntimeEnvironment.application.getString(expectedLabel) }
        verify(dateField).text = any()
        verify(preview).visibility = View.GONE
    }

}
