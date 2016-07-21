package de.qabel.qabelbox.box.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import de.qabel.qabelbox.R
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.helper.Formatter
import kotlinx.android.synthetic.main.item_files.view.*

open class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    open fun bindTo(entry: BrowserEntry): Unit = with(entry) {
        itemView.entryName.text = entry.name
        if (this is BrowserEntry.File) {
            itemView.modificationTime.text = Formatter.formatDateTimeString(mTime.time)
        }
        itemView.fileEntryIcon.setImageResource(
                when (this) {
                    is BrowserEntry.File -> R.drawable.file
                    is BrowserEntry.Folder -> R.drawable.folder
                })
    }

}
