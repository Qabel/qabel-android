package de.qabel.qabelbox.box.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import de.qabel.core.ui.displayName
import de.qabel.qabelbox.R
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.helper.Formatter
import de.qabel.qabelbox.ui.extensions.setOrGone
import de.qabel.qabelbox.ui.extensions.setVisibleOrGone
import kotlinx.android.synthetic.main.item_files.view.*

open class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    open fun bindTo(entry: BrowserEntry): Unit = with(itemView) {
        entryName.text = entry.name
        val detailsVisible = entry is BrowserEntry.File
        var sizeText = ""
        if (detailsVisible) {
            val fileEntry = entry as BrowserEntry.File
            modificationTime.text = Formatter.formatDateTimeString(fileEntry.mTime.time, context)
            extraDetails.setOrGone(createShareLabel(entry))
            sizeText = android.text.format.Formatter.formatShortFileSize(context, entry.size)
        }
        entrySize.setOrGone(sizeText)
        details.setVisibleOrGone(detailsVisible)
        fileEntryIcon.setImageResource(
                when (entry) {
                    is BrowserEntry.File -> R.drawable.file
                    is BrowserEntry.Folder -> R.drawable.folder
                })
    }

    private fun createShareLabel(entry: BrowserEntry) = if (entry.sharedTo.isNotEmpty()) entry.sharedTo.map {
        it?.displayName() ?: itemView.context.getString(R.string.unknown)
    }.joinToString().let {
        itemView.context.getString(R.string.filebrowser_file_is_shared_to) + " " + it
    } else ""

}
