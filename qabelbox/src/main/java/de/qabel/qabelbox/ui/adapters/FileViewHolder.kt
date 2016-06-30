package de.qabel.qabelbox.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import de.qabel.qabelbox.R
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.dto.BrowserEntry
import de.qabel.qabelbox.dto.MessagePayload
import de.qabel.qabelbox.helper.Formatter
import kotlinx.android.synthetic.main.item_chat_message_in.view.*
import kotlinx.android.synthetic.main.item_contacts.view.*
import kotlinx.android.synthetic.main.item_files.view.*

open class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    open fun bindTo(entry: BrowserEntry): Unit = with(entry) {
        itemView.entryName.text = entry.name
        when (this) {
            is BrowserEntry.File -> {
                itemView.modificationTime.text = Formatter.formatDateTimeString(mTime.time)
                itemView.fileEntryIcon.setImageResource(R.drawable.file)
            }
            is BrowserEntry.Folder -> {
                itemView.fileEntryIcon.setImageResource(R.drawable.folder)
            }
        }
    }

}
