package de.qabel.qabelbox.box.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import de.qabel.qabelbox.R
import de.qabel.qabelbox.box.dto.BrowserEntry

open class FileAdapter(var entries: MutableList<BrowserEntry>,
                       val click: (BrowserEntry) -> Unit = {},
                       val longClick: (BrowserEntry) -> Unit = {}):
        RecyclerView.Adapter<FileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): FileViewHolder? {
        parent ?: throw IllegalArgumentException("Parent view group is null")

        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_files, parent, false)
        val holder = FileViewHolder(layout)

        return holder
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getItemCount(): Int = entries.size

    override fun onBindViewHolder(holder: FileViewHolder?, position: Int) {
        holder ?: return
        val entry = getItemAtPosition(position) ?: return

        holder.bindTo(entry)
        with(holder.itemView) {
            setOnClickListener { click(entry) }
            setOnLongClickListener {
                longClick(entry)
                true
            }
        }
    }

    fun getItemAtPosition(position: Int): BrowserEntry? = entries.getOrNull(position)

}

