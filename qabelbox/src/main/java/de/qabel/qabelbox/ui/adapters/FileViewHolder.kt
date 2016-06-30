package de.qabel.qabelbox.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.dto.MessagePayload
import de.qabel.qabelbox.helper.Formatter
import kotlinx.android.synthetic.main.item_chat_message_in.view.*

open class FileViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {

    open fun bindTo(file: Any): Unit = TODO()

}


