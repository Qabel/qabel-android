package de.qabel.qabelbox.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import de.qabel.qabelbox.R
import de.qabel.qabelbox.dto.FileEntry

open class FileAdapter(var files: MutableList<FileEntry>): RecyclerView.Adapter<FileViewHolder>() {

    companion object {
        const val NO_MESSAGE = R.layout.item_no_message
        const val INCOMING_TEXT = R.layout.item_chat_message_in
        const val INCOMING_SHARE = R.layout.item_share_message_in
        const val OUTGOING_TEXT = R.layout.item_chat_message_out
        const val OUTGOING_SHARE = R.layout.item_share_message_out
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): FileViewHolder? {
        parent ?: throw IllegalArgumentException("Parent view group is null")

        val layout = LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false);
        return FileViewHolder(layout)
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: FileViewHolder?, position: Int) {
        holder?.bindTo(getItemAtPosition(position) ?: return)
    }

    fun getItemAtPosition(position: Int): FileEntry? = files.getOrNull(position)

}

