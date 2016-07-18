package de.qabel.qabelbox.chat.view.adapters

import android.view.View
import android.widget.LinearLayout
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.dto.MessagePayload
import de.qabel.qabelbox.ui.views.TextViewFont
import kotlinx.android.synthetic.main.chat_message_in.view.*
import kotlinx.android.synthetic.main.chat_message_text.view.*
import org.junit.Test
import java.util.*

class TextChatMessageViewHolderTest {

    @Test
    fun viewHolderCanBindItself() {
        val contact = mock<Contact>()
        stub(contact.alias).toReturn("contact")

        val view = mock<View>()
        val contentView = mock<LinearLayout>();
        val textField: TextViewFont = mock()
        val dateField: TextViewFont = mock()
        stub(view.tvText).toReturn(textField)
        stub(view.tvDate).toReturn(dateField)
        stub(view.chatContent).toReturn(contentView);

        val holder = TextChatMessageViewHolder(view)
        val msg = ChatMessage(mock<Identity>(), contact,
                ChatMessage.Direction.INCOMING, Date(), MessagePayload.TextMessage("text"))
        holder.bindTo(msg, false)

        verify(textField).text = "text"
        verify(dateField).text  = any()
    }

}
