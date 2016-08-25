package de.qabel.qabelbox.chat.view.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dto.ChatConversationDto
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.ui.DataViewAdapter


open class ChatOverviewAdapter(val clickListener: (ChatMessage) -> Unit,
                               val longClickListener: (ChatMessage) -> Boolean) :
        RecyclerView.Adapter<ConversationViewHolder>(), DataViewAdapter<ChatConversationDto> {

    override var data: MutableList<ChatConversationDto> = mutableListOf()

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) =
            holder.bindTo(data[position])

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder =
            ConversationViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chatoverview, parent, false), clickListener, longClickListener)

    override fun getItemCount(): Int = data.size

    override fun notifyView() = notifyDataSetChanged()

    override fun notifyViewRange(start: Int, count: Int) = notifyItemRangeChanged(start, count)

}
