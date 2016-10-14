package de.qabel.qabelbox.chat.view.adapters

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.View
import android.widget.LinearLayout
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayloadDto
import de.qabel.qabelbox.test.shadows.TextViewFontShadow
import de.qabel.qabelbox.ui.views.TextViewFont
import kotlinx.android.synthetic.main.chat_message_in.view.*
import kotlinx.android.synthetic.main.chat_message_text.view.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDateFormat
import org.robolectric.shadows.ShadowSystemClock
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class,
        shadows = arrayOf(TextViewFontShadow::class, ShadowDateFormat::class), manifest = "src/main/AndroidManifest.xml")
class TextChatMessageViewHolderTest {

    @Test
    fun viewHolderCanBindItself() {
        val contact = mock<Contact>()
        stub(contact.alias).toReturn("contact")

        val view = mock<View>()
        stub(view.context).toReturn(RuntimeEnvironment.application)
        val textField: TextViewFont = mock()
        val dateField: TextViewFont = mock()
        val messageContainer: LinearLayout = mock()
        val background = mock<Drawable>().apply {
            stub(this.mutate()).toReturn(mock<GradientDrawable>())
        }
        stub(messageContainer.background).toReturn(background)
        stub(view.tvDate).toReturn(dateField)
        stub(view.tvText).toReturn(textField)
        stub(view.msg_container).toReturn(messageContainer)
        stub(view.contact_avatar).toReturn(mock())

        val date = Date()
        ShadowSystemClock.setCurrentTimeMillis(date.time)
        val holder = TextChatMessageViewHolder(view)
        val msg = ChatMessage(mock<Identity>(), contact,
                ChatDropMessage.Direction.INCOMING, date, MessagePayloadDto.TextMessage("text"))
        holder.bindTo(msg, false)

        verify(textField).text = "text"
        verify(dateField).text = RuntimeEnvironment.application.getString(R.string.moments_ago)
    }

}
